package com.vaibhav.runbookos.security;

import com.vaibhav.runbookos.config.RunbookOsProperties;
import com.vaibhav.runbookos.exception.AuthenticationDomainException;
import com.vaibhav.runbookos.identity.UserRepository;
import com.vaibhav.runbookos.organization.MembershipRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService service;
  private final UserRepository users;
  private final MembershipRepository memberships;
  private final RunbookOsProperties.Security properties;

  public AuthController(
      AuthService service,
      UserRepository users,
      MembershipRepository memberships,
      RunbookOsProperties properties) {
    this.service = service;
    this.users = users;
    this.memberships = memberships;
    this.properties = properties.security();
  }

  @PostMapping("/signup")
  public ResponseEntity<AuthResponse> signup(
      @Valid @RequestBody SignupRequest request, HttpServletRequest http) {
    return response(
        service.signup(
            request.email(),
            request.password(),
            request.displayName(),
            http.getHeader("User-Agent"),
            http.getRemoteAddr()));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest http) {
    return response(
        service.login(
            request.email(),
            request.password(),
            http.getHeader("User-Agent"),
            http.getRemoteAddr()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(HttpServletRequest http) {
    return response(
        service.refresh(refreshCookie(http), http.getHeader("User-Agent"), http.getRemoteAddr()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      HttpServletRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
    service.logout(refreshCookie(request), principal.userId());
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, clearCookie().toString())
        .build();
  }

  @GetMapping("/me")
  public CurrentUserResponse current(@AuthenticationPrincipal AuthenticatedUser principal) {
    var user =
        users
            .findById(principal.userId())
            .orElseThrow(AuthenticationDomainException::invalidCredentials);
    List<OrganizationMembershipResponse> organizations =
        memberships.findActiveMembershipsWithOrganizations(principal.userId()).stream()
            .map(
                v ->
                    new OrganizationMembershipResponse(
                        v.organizationId(),
                        v.organizationName(),
                        v.organizationSlug(),
                        v.demoMode(),
                        v.role().name()))
            .toList();
    return new CurrentUserResponse(
        user.getId(),
        user.getEmail(),
        user.getDisplayName(),
        principal.organizationId(),
        organizations);
  }

  private ResponseEntity<AuthResponse> response(AuthService.SessionTokens tokens) {
    ResponseCookie cookie =
        ResponseCookie.from(properties.refreshCookieName(), tokens.refreshToken())
            .httpOnly(true)
            .secure(properties.refreshCookieSecure())
            .sameSite("Strict")
            .path("/api/auth")
            .maxAge(properties.refreshTokenTtl())
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new AuthResponse(tokens.accessToken(), tokens.expiresIn(), tokens.user()));
  }

  private ResponseCookie clearCookie() {
    return ResponseCookie.from(properties.refreshCookieName(), "")
        .httpOnly(true)
        .secure(properties.refreshCookieSecure())
        .sameSite("Strict")
        .path("/api/auth")
        .maxAge(Duration.ZERO)
        .build();
  }

  private String refreshCookie(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }
    return java.util.Arrays.stream(request.getCookies())
        .filter(cookie -> properties.refreshCookieName().equals(cookie.getName()))
        .map(jakarta.servlet.http.Cookie::getValue)
        .findFirst()
        .orElse(null);
  }

  public record SignupRequest(
      @Email(message = "Enter a valid email address")
          @NotBlank(message = "Email address is required")
          String email,
      @NotBlank(message = "Password is required")
          @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
          String password,
      @NotBlank(message = "Name is required")
          @Size(max = 120, message = "Name must be 120 characters or fewer")
          String displayName) {}

  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

  public record AuthResponse(String accessToken, long expiresIn, AuthService.UserView user) {}

  public record CurrentUserResponse(
      UUID id,
      String email,
      String displayName,
      UUID selectedOrganizationId,
      List<OrganizationMembershipResponse> organizations) {}

  public record OrganizationMembershipResponse(
      UUID id, String name, String slug, boolean demoMode, String role) {}
}
