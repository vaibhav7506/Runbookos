package com.vaibhav.runbookos.integration.dto;

import com.vaibhav.runbookos.integration.IntegrationCredentialReference;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * What the API is willing to say about a stored secret.
 *
 * <p>There is no field for the value, by design. The fingerprint is a truncated SHA-256 digest that
 * lets an operator confirm which secret is installed without it ever being transmitted.
 */
@Schema(description = "Metadata about a stored integration secret. The value is never returned.")
public record CredentialSummaryResponse(
    @Schema(description = "Identifier of the credential record") UUID id,
    @Schema(description = "Which secret this is within the integration", example = "token")
        String credentialKey,
    @Schema(
            description = "Non-reversible short digest for operator confirmation",
            example = "9f2a4c1b7e03")
        String fingerprint,
    @Schema(description = "Master key used, for rotation tracking", example = "primary")
        String keyId,
    @Schema(description = "True once the secret has been revoked and overwritten") boolean revoked,
    @Schema(description = "When the secret was first stored") Instant createdAt,
    @Schema(description = "When the secret was last rotated or revoked") Instant updatedAt) {

  public static CredentialSummaryResponse from(IntegrationCredentialReference credential) {
    return new CredentialSummaryResponse(
        credential.getId(),
        credential.getCredentialKey(),
        credential.getFingerprint(),
        credential.getKeyId(),
        credential.isRevoked(),
        credential.getCreatedAt(),
        credential.getUpdatedAt());
  }
}
