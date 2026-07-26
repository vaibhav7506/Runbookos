package com.vaibhav.runbookos.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Encrypted secret material belonging to an {@link Integration}.
 *
 * <p>The ciphertext is AES-256-GCM and is only ever decrypted inside the integration layer at the
 * moment of use. No controller, DTO, or log statement exposes it. {@link #fingerprint} is a
 * non-reversible short digest that lets an operator confirm which secret is installed without
 * revealing it.
 */
@Entity
@Table(name = "integration_credential_references")
public class IntegrationCredentialReference {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Column(name = "integration_id", nullable = false, updatable = false)
  private UUID integrationId;

  /**
   * Which secret this is within the integration, for example {@code token} or {@code
   * signing_secret}.
   */
  @Column(name = "credential_key", nullable = false, updatable = false, length = 64)
  private String credentialKey;

  @Column(nullable = false, columnDefinition = "text")
  private String ciphertext;

  @Column(nullable = false, length = 64)
  private String nonce;

  /** Which master key encrypted this value, so keys can be rotated. */
  @Column(name = "key_id", nullable = false, length = 64)
  private String keyId;

  @Column(nullable = false, length = 12)
  private String fingerprint;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected IntegrationCredentialReference() {
    // Required by JPA.
  }

  private IntegrationCredentialReference(
      UUID organizationId,
      UUID integrationId,
      String credentialKey,
      String ciphertext,
      String nonce,
      String keyId,
      String fingerprint,
      Instant now) {
    this.id = UUID.randomUUID();
    this.organizationId = organizationId;
    this.integrationId = integrationId;
    this.credentialKey = credentialKey;
    this.ciphertext = ciphertext;
    this.nonce = nonce;
    this.keyId = keyId;
    this.fingerprint = fingerprint;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static IntegrationCredentialReference create(
      UUID organizationId,
      UUID integrationId,
      String credentialKey,
      String ciphertext,
      String nonce,
      String keyId,
      String fingerprint,
      Instant now) {
    return new IntegrationCredentialReference(
        organizationId, integrationId, credentialKey, ciphertext, nonce, keyId, fingerprint, now);
  }

  /** Replaces the stored secret in place, preserving the credential's identity and history. */
  public void rotate(
      String newCiphertext, String newNonce, String newKeyId, String newFingerprint, Instant at) {
    this.ciphertext = newCiphertext;
    this.nonce = newNonce;
    this.keyId = newKeyId;
    this.fingerprint = newFingerprint;
    this.revokedAt = null;
    this.updatedAt = at;
  }

  /**
   * Marks the credential unusable. The ciphertext is overwritten rather than merely flagged, so a
   * disconnected integration leaves no recoverable secret behind.
   */
  public void revoke(Instant at) {
    this.revokedAt = at;
    this.ciphertext = "";
    this.nonce = "";
    this.updatedAt = at;
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getIntegrationId() {
    return integrationId;
  }

  public String getCredentialKey() {
    return credentialKey;
  }

  public String getCiphertext() {
    return ciphertext;
  }

  public String getNonce() {
    return nonce;
  }

  public String getKeyId() {
    return keyId;
  }

  public String getFingerprint() {
    return fingerprint;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof IntegrationCredentialReference reference
        && id != null
        && id.equals(reference.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  /** Excludes ciphertext and nonce. */
  @Override
  public String toString() {
    return "IntegrationCredentialReference[id=%s, key=%s, fingerprint=%s, revoked=%s]"
        .formatted(id, credentialKey, fingerprint, revokedAt != null);
  }
}
