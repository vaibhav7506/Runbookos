package com.vaibhav.runbookos.analysis;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_provider_configs")
public class AiProviderConfig {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AiProvider provider;

  @Column(nullable = false, length = 120)
  private String model;

  @Column(name = "base_url", length = 512)
  private String baseUrl;

  @Column(name = "encrypted_api_key")
  private String encryptedApiKey;

  @Column(name = "key_nonce")
  private String keyNonce;

  @Column(name = "key_id", length = 64)
  private String keyId;

  @Column(name = "key_fingerprint", length = 64)
  private String keyFingerprint;

  @Column(nullable = false)
  private int priority;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "timeout_seconds", nullable = false)
  private int timeoutSeconds;

  @Column(name = "max_retries", nullable = false)
  private int maxRetries;

  @Column(name = "input_token_budget", nullable = false)
  private int inputTokenBudget;

  @Column(name = "output_token_budget", nullable = false)
  private int outputTokenBudget;

  @Column(name = "cost_budget_usd", nullable = false, precision = 12, scale = 6)
  private BigDecimal costBudgetUsd;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected AiProviderConfig() {}

  public static AiProviderConfig create(
      UUID org,
      AiProvider provider,
      String model,
      String baseUrl,
      String encryptedKey,
      String nonce,
      String keyId,
      String fingerprint,
      int priority,
      int timeout,
      int retries,
      int inputBudget,
      int outputBudget,
      BigDecimal costBudget,
      UUID actor,
      Instant now) {
    AiProviderConfig value = new AiProviderConfig();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.provider = provider;
    value.model = model;
    value.baseUrl = baseUrl;
    value.encryptedApiKey = encryptedKey;
    value.keyNonce = nonce;
    value.keyId = keyId;
    value.keyFingerprint = fingerprint;
    value.priority = priority;
    value.enabled = true;
    value.timeoutSeconds = timeout;
    value.maxRetries = retries;
    value.inputTokenBudget = inputBudget;
    value.outputTokenBudget = outputBudget;
    value.costBudgetUsd = costBudget;
    value.createdBy = actor;
    value.createdAt = now;
    value.updatedAt = now;
    return value;
  }

  public void disable(Instant now) {
    enabled = false;
    updatedAt = now;
    encryptedApiKey = null;
    keyNonce = null;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public AiProvider getProvider() {
    return provider;
  }

  public String getModel() {
    return model;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getEncryptedApiKey() {
    return encryptedApiKey;
  }

  public String getKeyNonce() {
    return keyNonce;
  }

  public String getKeyId() {
    return keyId;
  }

  public String getKeyFingerprint() {
    return keyFingerprint;
  }

  public int getPriority() {
    return priority;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public int getInputTokenBudget() {
    return inputTokenBudget;
  }

  public int getOutputTokenBudget() {
    return outputTokenBudget;
  }

  public BigDecimal getCostBudgetUsd() {
    return costBudgetUsd;
  }
}
