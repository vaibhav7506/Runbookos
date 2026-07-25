package com.vaibhav.runbookos.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration for the control plane.
 *
 * <p>Every secret here is bound from an environment variable with no default, so the application
 * fails to start rather than silently running with a well-known key. {@code .env.example} carries
 * placeholder values only.
 *
 * @param security signing and encryption material
 * @param n8n workflow engine connection
 * @param cors browser origins permitted to call the API
 */
@Validated
@ConfigurationProperties(prefix = "runbookos")
public record RunbookOsProperties(@Valid Security security, @Valid N8n n8n, @Valid Cors cors) {

  /**
   * @param jwtSecret Base64-encoded HMAC key for access tokens, at least 32 bytes when decoded
   * @param jwtIssuer issuer claim written into and verified on every access token
   * @param accessTokenTtl lifetime of an access token; short by design because it cannot be revoked
   * @param refreshTokenTtl lifetime of a refresh session before re-authentication is required
   * @param encryptionKey Base64-encoded 32-byte AES-256 key for integration secrets
   * @param encryptionKeyId identifier stored next to each ciphertext to support key rotation
   * @param refreshCookieName name of the httpOnly cookie carrying the refresh token
   * @param refreshCookieSecure whether the refresh cookie requires HTTPS; false only for local dev
   */
  public record Security(
      @NotBlank String jwtSecret,
      @DefaultValue("runbookos") @NotBlank String jwtIssuer,
      @DefaultValue("15m") Duration accessTokenTtl,
      @DefaultValue("14d") Duration refreshTokenTtl,
      @NotBlank String encryptionKey,
      @DefaultValue("primary") @NotBlank String encryptionKeyId,
      @DefaultValue("runbookos_refresh") @NotBlank String refreshCookieName,
      @DefaultValue("true") boolean refreshCookieSecure,
      @DefaultValue("12") @Min(10) int passwordHashStrength) {}

  /**
   * @param baseUrl root URL of the n8n instance
   */
  public record N8n(@DefaultValue("http://localhost:5678") @NotBlank String baseUrl) {}

  /**
   * @param allowedOrigins exact origins allowed to send credentialed requests; wildcards are not
   *     permitted because the refresh cookie is sent with credentials
   */
  public record Cors(@DefaultValue("http://localhost:3000") List<String> allowedOrigins) {}
}
