package com.vaibhav.runbookos.integration;

import com.vaibhav.runbookos.audit.AuditActions;
import com.vaibhav.runbookos.audit.AuditRecord;
import com.vaibhav.runbookos.audit.AuditService;
import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.ResourceNotFoundException;
import com.vaibhav.runbookos.integration.dto.CredentialSummaryResponse;
import com.vaibhav.runbookos.security.crypto.SecretCipher;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores and retrieves integration secrets.
 *
 * <p>This is a deliberately one-way door for callers outside the integration layer. {@link #store}
 * and {@link #rotate} accept plaintext and return only a {@link CredentialSummaryResponse} carrying
 * a fingerprint. There is no method that returns a secret to a controller. {@link #revealForUse} is
 * package-private so only integration execution code can reach it.
 */
@Service
public class IntegrationCredentialService {

  private final IntegrationCredentialReferenceRepository credentialRepository;
  private final IntegrationRepository integrationRepository;
  private final SecretCipher secretCipher;
  private final AuditService auditService;
  private final TimeProvider timeProvider;

  public IntegrationCredentialService(
      IntegrationCredentialReferenceRepository credentialRepository,
      IntegrationRepository integrationRepository,
      SecretCipher secretCipher,
      AuditService auditService,
      TimeProvider timeProvider) {
    this.credentialRepository = credentialRepository;
    this.integrationRepository = integrationRepository;
    this.secretCipher = secretCipher;
    this.auditService = auditService;
    this.timeProvider = timeProvider;
  }

  /**
   * Stores a new secret, or rotates the existing one if this credential key is already present.
   *
   * @param plaintext the secret value; not logged, not echoed back, and not retained in memory
   *     beyond this call
   * @return a summary with a fingerprint only, never the value
   */
  @Transactional
  public CredentialSummaryResponse store(
      UUID organizationId, UUID integrationId, String credentialKey, String plaintext, UUID actor) {
    Integration integration = requireIntegration(organizationId, integrationId);
    Instant now = timeProvider.nowTruncated();
    SecretCipher.EncryptedValue encrypted = secretCipher.encrypt(plaintext);

    IntegrationCredentialReference existing =
        credentialRepository
            .findByIntegrationIdAndCredentialKey(integration.getId(), credentialKey)
            .orElse(null);

    boolean rotated = existing != null;
    IntegrationCredentialReference credential;
    if (rotated) {
      existing.rotate(
          encrypted.ciphertext(),
          encrypted.nonce(),
          encrypted.keyId(),
          encrypted.fingerprint(),
          now);
      credential = existing;
    } else {
      credential =
          credentialRepository.save(
              IntegrationCredentialReference.create(
                  organizationId,
                  integration.getId(),
                  credentialKey,
                  encrypted.ciphertext(),
                  encrypted.nonce(),
                  encrypted.keyId(),
                  encrypted.fingerprint(),
                  now));
    }

    auditService.record(
        AuditRecord.builder(
                rotated ? AuditActions.CREDENTIAL_ROTATED : AuditActions.CREDENTIAL_STORED,
                "integration_credential")
            .organization(organizationId)
            .actor(actor, null)
            .resourceId(credential.getId())
            .metadata("integrationId", integration.getId().toString())
            .metadata("credentialKey", credentialKey)
            .metadata("fingerprint", credential.getFingerprint())
            .metadata("keyId", credential.getKeyId())
            .build());

    return CredentialSummaryResponse.from(credential);
  }

  /** Explicit rotation; behaves as {@link #store} but requires the credential to already exist. */
  @Transactional
  public CredentialSummaryResponse rotate(
      UUID organizationId, UUID integrationId, String credentialKey, String plaintext, UUID actor) {
    requireIntegration(organizationId, integrationId);
    credentialRepository
        .findByIntegrationIdAndCredentialKey(integrationId, credentialKey)
        .orElseThrow(() -> new ResourceNotFoundException("integration credential", credentialKey));
    return store(organizationId, integrationId, credentialKey, plaintext, actor);
  }

  /** Lists which secrets exist and their fingerprints. Values are never included. */
  @Transactional(readOnly = true)
  public List<CredentialSummaryResponse> list(UUID organizationId, UUID integrationId) {
    Integration integration = requireIntegration(organizationId, integrationId);
    return credentialRepository.findByIntegrationId(integration.getId()).stream()
        .map(CredentialSummaryResponse::from)
        .toList();
  }

  /** Revokes a secret and overwrites its ciphertext so nothing recoverable is left behind. */
  @Transactional
  public void revoke(UUID organizationId, UUID integrationId, String credentialKey, UUID actor) {
    Integration integration = requireIntegration(organizationId, integrationId);
    IntegrationCredentialReference credential =
        credentialRepository
            .findByIntegrationIdAndCredentialKey(integration.getId(), credentialKey)
            .orElseThrow(
                () -> new ResourceNotFoundException("integration credential", credentialKey));
    credential.revoke(timeProvider.nowTruncated());

    auditService.record(
        AuditRecord.builder(AuditActions.CREDENTIAL_REVOKED, "integration_credential")
            .organization(organizationId)
            .actor(actor, null)
            .resourceId(credential.getId())
            .metadata("integrationId", integration.getId().toString())
            .metadata("credentialKey", credentialKey)
            .build());
  }

  /**
   * Decrypts a secret for immediate use by integration execution code. Package-private on purpose:
   * widening this to public would let a controller return a secret.
   *
   * @throws ResourceNotFoundException if the credential is absent or revoked
   */
  @Transactional(readOnly = true)
  String revealForUse(UUID organizationId, UUID integrationId, String credentialKey) {
    Integration integration = requireIntegration(organizationId, integrationId);
    IntegrationCredentialReference credential =
        credentialRepository
            .findByIntegrationIdAndCredentialKey(integration.getId(), credentialKey)
            .filter(reference -> !reference.isRevoked())
            .orElseThrow(
                () -> new ResourceNotFoundException("integration credential", credentialKey));
    return secretCipher.decrypt(credential.getCiphertext(), credential.getNonce());
  }

  private Integration requireIntegration(UUID organizationId, UUID integrationId) {
    return integrationRepository
        .findByIdAndOrganizationId(integrationId, organizationId)
        .orElseThrow(() -> new ResourceNotFoundException("integration", integrationId));
  }
}
