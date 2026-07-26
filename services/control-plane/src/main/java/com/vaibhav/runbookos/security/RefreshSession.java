package com.vaibhav.runbookos.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A server-side refresh session. Only the SHA-256 hash of the opaque token is stored, so a database
 * disclosure does not yield usable tokens.
 *
 * <p>Rotation forms a chain via {@link #previousId}: each refresh revokes the presented session and
 * issues a successor. Presenting an already-rotated token means the token leaked, so the whole
 * chain for that user is revoked rather than just the one record.
 */
@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
  private String tokenHash;

  @Column(name = "previous_id", updatable = false)
  private UUID previousId;

  @Column(name = "issued_at", nullable = false, updatable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "revoked_reason", length = 64)
  private String revokedReason;

  @Column(name = "user_agent", length = 255)
  private String userAgent;

  @Column(name = "ip_address", length = 64)
  private String ipAddress;

  protected RefreshSession() {
    // Required by JPA.
  }

  private RefreshSession(
      UUID id,
      UUID userId,
      String tokenHash,
      UUID previousId,
      Instant issuedAt,
      Instant expiresAt,
      String userAgent,
      String ipAddress) {
    this.id = id;
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.previousId = previousId;
    this.issuedAt = issuedAt;
    this.expiresAt = expiresAt;
    this.userAgent = userAgent;
    this.ipAddress = ipAddress;
  }

  public static RefreshSession issue(
      UUID userId,
      String tokenHash,
      UUID previousId,
      Instant issuedAt,
      Instant expiresAt,
      String userAgent,
      String ipAddress) {
    return new RefreshSession(
        UUID.randomUUID(),
        userId,
        tokenHash,
        previousId,
        issuedAt,
        expiresAt,
        truncate(userAgent, 255),
        truncate(ipAddress, 64));
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return null;
    }
    return value.length() <= max ? value : value.substring(0, max);
  }

  public void revoke(RevocationReason reason, Instant at) {
    // Preserve the original reason: the first revocation explains why the chain ended.
    if (this.revokedAt == null) {
      this.revokedAt = at;
      this.revokedReason = reason.name();
    }
  }

  public boolean isActiveAt(Instant at) {
    return revokedAt == null && at.isBefore(expiresAt);
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public UUID getPreviousId() {
    return previousId;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public String getRevokedReason() {
    return revokedReason;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof RefreshSession session && id != null && id.equals(session.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  /** Excludes the token hash. */
  @Override
  public String toString() {
    return "RefreshSession[id=%s, user=%s, revoked=%s]".formatted(id, userId, revokedAt != null);
  }

  /** Why a refresh session stopped being usable. Recorded for audit and incident review. */
  public enum RevocationReason {
    /** Normal rotation: the holder exchanged this token for a successor. */
    ROTATED,
    /** The user signed out. */
    LOGOUT,
    /** A rotated token was replayed, so the entire chain was invalidated. */
    REUSE_DETECTED,
    /** An administrator or password change invalidated all sessions. */
    ADMIN_REVOKED
  }
}
