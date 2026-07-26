package com.vaibhav.runbookos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vaibhav.runbookos.config.RunbookOsProperties;
import com.vaibhav.runbookos.exception.AuthenticationDomainException;
import com.vaibhav.runbookos.identity.Role;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
  private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

  @Test
  void roundTripsUserTenantAndRole() {
    JwtService service = new JwtService(properties("a-secure-jwt-secret-that-is-long-enough"));
    UUID user = UUID.randomUUID();
    UUID organization = UUID.randomUUID();

    AuthenticatedUser result =
        service.verify(service.issue(user, organization, Role.RESPONDER, NOW), NOW.plusSeconds(1));

    assertThat(result.userId()).isEqualTo(user);
    assertThat(result.organizationId()).isEqualTo(organization);
    assertThat(result.role()).isEqualTo(Role.RESPONDER);
  }

  @Test
  void rejectsExpiredAndTamperedTokens() {
    JwtService service = new JwtService(properties("a-secure-jwt-secret-that-is-long-enough"));
    String token = service.issue(UUID.randomUUID(), null, null, NOW);
    assertThatThrownBy(() -> service.verify(token, NOW.plus(Duration.ofMinutes(16))))
        .isInstanceOf(AuthenticationDomainException.class);
    assertThatThrownBy(() -> service.verify(token + "tampered", NOW))
        .isInstanceOf(AuthenticationDomainException.class);
  }

  private static RunbookOsProperties properties(String jwtSecret) {
    return new RunbookOsProperties(
        new RunbookOsProperties.Security(
            jwtSecret,
            "runbookos-test",
            Duration.ofMinutes(15),
            Duration.ofDays(14),
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "test",
            "refresh",
            false,
            10),
        new RunbookOsProperties.N8n(
            "http://localhost:5678",
            "test-internal-secret-that-is-at-least-32-bytes",
            Duration.ofSeconds(30),
            Duration.ofSeconds(10)),
        new RunbookOsProperties.Cors(List.of("http://localhost:3000")));
  }
}
