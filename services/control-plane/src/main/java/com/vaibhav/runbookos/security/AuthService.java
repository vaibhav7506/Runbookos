package com.vaibhav.runbookos.security;

import com.vaibhav.runbookos.audit.ActorType;
import com.vaibhav.runbookos.audit.AuditActions;
import com.vaibhav.runbookos.audit.AuditRecord;
import com.vaibhav.runbookos.audit.AuditService;
import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.config.RunbookOsProperties;
import com.vaibhav.runbookos.exception.AuthenticationDomainException;
import com.vaibhav.runbookos.exception.ConflictException;
import com.vaibhav.runbookos.identity.User;
import com.vaibhav.runbookos.identity.UserRepository;
import com.vaibhav.runbookos.organization.Membership;
import com.vaibhav.runbookos.organization.MembershipRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final MembershipRepository memberships;
  private final RefreshSessionRepository sessions;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuditService auditService;
  private final TimeProvider timeProvider;
  private final RunbookOsProperties.Security properties;
  private final SecureRandom random = new SecureRandom();

  public AuthService(
      UserRepository users,
      MembershipRepository memberships,
      RefreshSessionRepository sessions,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      AuditService auditService,
      TimeProvider timeProvider,
      RunbookOsProperties properties) {
    this.users = users;
    this.memberships = memberships;
    this.sessions = sessions;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.auditService = auditService;
    this.timeProvider = timeProvider;
    this.properties = properties.security();
  }

  @Transactional
  public SessionTokens signup(
      String email, String password, String displayName, String agent, String ip) {
    String normalized = User.normalizeEmail(email);
    if (users.existsByEmailNormalized(normalized)) {
      throw new ConflictException(
          "EMAIL_ALREADY_REGISTERED", "An account already exists for this email");
    }
    Instant now = timeProvider.nowTruncated();
    User user =
        users.save(
            User.create(email.trim(), passwordEncoder.encode(password), displayName.trim(), now));
    auditService.record(
        AuditRecord.builder(AuditActions.USER_SIGNED_UP, "user")
            .actor(user.getId(), user.getDisplayName())
            .resourceId(user.getId())
            .build());
    return issue(user, null, agent, ip, now);
  }

  @Transactional
  public SessionTokens login(String email, String password, String agent, String ip) {
    User user = users.findByEmailNormalized(User.normalizeEmail(email)).orElse(null);
    if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
      auditService.record(
          AuditRecord.builder(AuditActions.USER_LOGIN_FAILED, "user")
              .actorType(ActorType.ANONYMOUS)
              .actorLabel("authentication attempt")
              .outcome(com.vaibhav.runbookos.audit.AuditOutcome.FAILURE)
              .reason("Invalid credentials")
              .build());
      throw AuthenticationDomainException.invalidCredentials();
    }
    if (!user.isActive()) {
      throw AuthenticationDomainException.accountNotActive();
    }
    Instant now = timeProvider.nowTruncated();
    user.recordLogin(now);
    Membership selected =
        memberships.findByUserId(user.getId()).stream()
            .filter(Membership::isActive)
            .findFirst()
            .orElse(null);
    auditService.recordSuccess(
        AuditActions.USER_LOGIN_SUCCEEDED,
        "user",
        selected == null ? null : selected.getOrganizationId(),
        user.getId(),
        user.getId());
    return issue(user, selected, agent, ip, now);
  }

  @Transactional
  public SessionTokens refresh(String rawToken, String agent, String ip) {
    Instant now = timeProvider.nowTruncated();
    RefreshSession session =
        sessions
            .findByTokenHash(hash(rawToken))
            .orElseThrow(AuthenticationDomainException::invalidRefreshSession);
    if (session.isRevoked()) {
      sessions.revokeAllForUser(
          session.getUserId(), now, RefreshSession.RevocationReason.REUSE_DETECTED.name());
      auditService.record(
          AuditRecord.builder(AuditActions.SESSION_REUSE_DETECTED, "refresh_session")
              .actor(session.getUserId(), null)
              .resourceId(session.getId())
              .outcome(com.vaibhav.runbookos.audit.AuditOutcome.DENIED)
              .reason("A rotated refresh token was replayed")
              .build());
      throw AuthenticationDomainException.invalidRefreshSession();
    }
    if (!session.isActiveAt(now)) {
      throw AuthenticationDomainException.invalidRefreshSession();
    }
    session.revoke(RefreshSession.RevocationReason.ROTATED, now);
    User user =
        users
            .findById(session.getUserId())
            .orElseThrow(AuthenticationDomainException::invalidRefreshSession);
    Membership selected =
        memberships.findByUserId(user.getId()).stream()
            .filter(Membership::isActive)
            .findFirst()
            .orElse(null);
    SessionTokens tokens = issue(user, selected, session.getId(), agent, ip, now);
    auditService.recordSuccess(
        AuditActions.SESSION_REFRESHED,
        "refresh_session",
        selected == null ? null : selected.getOrganizationId(),
        user.getId(),
        session.getId());
    return tokens;
  }

  @Transactional
  public void logout(String rawToken, UUID userId) {
    sessions
        .findByTokenHash(hash(rawToken))
        .filter(session -> session.getUserId().equals(userId))
        .ifPresent(
            session ->
                session.revoke(
                    RefreshSession.RevocationReason.LOGOUT, timeProvider.nowTruncated()));
    auditService.recordSuccess(AuditActions.USER_LOGGED_OUT, "user", null, userId, userId);
  }

  private SessionTokens issue(
      User user, Membership membership, String agent, String ip, Instant now) {
    return issue(user, membership, null, agent, ip, now);
  }

  private SessionTokens issue(
      User user, Membership membership, UUID previousId, String agent, String ip, Instant now) {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    sessions.save(
        RefreshSession.issue(
            user.getId(),
            hash(refresh),
            previousId,
            now,
            now.plus(properties.refreshTokenTtl()),
            agent,
            ip));
    String access =
        jwtService.issue(
            user.getId(),
            membership == null ? null : membership.getOrganizationId(),
            membership == null ? null : membership.getRole(),
            now);
    return new SessionTokens(
        access, refresh, properties.accessTokenTtl().toSeconds(), UserView.from(user));
  }

  static String hash(String token) {
    if (token == null || token.isBlank()) {
      throw AuthenticationDomainException.invalidRefreshSession();
    }
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public record SessionTokens(
      String accessToken, String refreshToken, long expiresIn, UserView user) {}

  public record UserView(UUID id, String email, String displayName) {
    static UserView from(User user) {
      return new UserView(user.getId(), user.getEmail(), user.getDisplayName());
    }
  }
}
