package com.vaibhav.runbookos.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A configured connection to an external system, scoped to one organization.
 *
 * <p>{@link #config} holds non-secret settings only, such as a repository name or base URL. Secret
 * material lives encrypted in {@link IntegrationCredentialReference} and is never held here.
 */
@Entity
@Table(name = "integrations")
public class Integration {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 32)
  private IntegrationKind kind;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private IntegrationEnvironment environment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private IntegrationStatus status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> config;

  @Column(name = "last_success_at")
  private Instant lastSuccessAt;

  @Column(name = "last_error_at")
  private Instant lastErrorAt;

  @Column(name = "last_error_message", length = 512)
  private String lastErrorMessage;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected Integration() {
    // Required by JPA.
  }

  private Integration(
      UUID organizationId,
      IntegrationKind kind,
      String name,
      IntegrationEnvironment environment,
      Map<String, Object> config,
      UUID createdBy,
      Instant now) {
    this.id = UUID.randomUUID();
    this.organizationId = organizationId;
    this.kind = kind;
    this.name = name;
    this.environment = environment;
    this.status = IntegrationStatus.PENDING;
    this.config = config == null ? Map.of() : Map.copyOf(config);
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static Integration create(
      UUID organizationId,
      IntegrationKind kind,
      String name,
      IntegrationEnvironment environment,
      Map<String, Object> config,
      UUID createdBy,
      Instant now) {
    return new Integration(organizationId, kind, name, environment, config, createdBy, now);
  }

  public void updateConfig(String newName, Map<String, Object> newConfig, Instant at) {
    this.name = newName;
    this.config = newConfig == null ? Map.of() : Map.copyOf(newConfig);
    this.updatedAt = at;
  }

  public void markConnected(Instant at) {
    this.status = IntegrationStatus.CONNECTED;
    this.lastSuccessAt = at;
    this.lastErrorMessage = null;
    this.updatedAt = at;
  }

  /**
   * Records a failure. The message is truncated and is expected to already be sanitised by the
   * caller; integration adapters must never pass raw credentials or response bodies here.
   */
  public void markError(String message, Instant at) {
    this.status = IntegrationStatus.ERROR;
    this.lastErrorAt = at;
    this.lastErrorMessage = message == null || message.length() <= 512
        ? message
        : message.substring(0, 512);
    this.updatedAt = at;
  }

  public void disconnect(Instant at) {
    this.status = IntegrationStatus.DISCONNECTED;
    this.updatedAt = at;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public IntegrationKind getKind() {
    return kind;
  }

  public String getName() {
    return name;
  }

  public IntegrationEnvironment getEnvironment() {
    return environment;
  }

  public IntegrationStatus getStatus() {
    return status;
  }

  public Map<String, Object> getConfig() {
    return config == null ? Map.of() : Map.copyOf(config);
  }

  public Instant getLastSuccessAt() {
    return lastSuccessAt;
  }

  public Instant getLastErrorAt() {
    return lastErrorAt;
  }

  public String getLastErrorMessage() {
    return lastErrorMessage;
  }

  public UUID getCreatedBy() {
    return createdBy;
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
    return other instanceof Integration integration && id != null && id.equals(integration.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return "Integration[id=%s, kind=%s, status=%s]".formatted(id, kind, status);
  }
}
