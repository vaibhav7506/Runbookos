package com.vaibhav.runbookos.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An append-only record of a security- or state-relevant event.
 *
 * <p>There are no setters and no {@code @Version} column: once written, an audit event is never
 * modified. The database additionally enforces this with triggers that reject UPDATE and DELETE, so
 * an application bug cannot silently rewrite history.
 *
 * <p>{@code organizationId} and {@code actorUserId} are intentionally not foreign keys, so audit
 * records outlive the entities they describe.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "organization_id", updatable = false)
  private UUID organizationId;

  @Column(name = "actor_user_id", updatable = false)
  private UUID actorUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "actor_type", nullable = false, updatable = false, length = 32)
  private ActorType actorType;

  @Column(name = "actor_label", updatable = false, length = 255)
  private String actorLabel;

  @Column(nullable = false, updatable = false, length = 96)
  private String action;

  @Column(name = "resource_type", nullable = false, updatable = false, length = 64)
  private String resourceType;

  @Column(name = "resource_id", updatable = false, length = 128)
  private String resourceId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 32)
  private AuditOutcome outcome;

  @Column(updatable = false, length = 512)
  private String reason;

  @Column(name = "correlation_id", updatable = false, length = 64)
  private String correlationId;

  @Column(name = "ip_address", updatable = false, length = 64)
  private String ipAddress;

  /**
   * Free-form, non-sensitive context. Mapped with Hibernate's native JSON support, so no
   * third-party type library is required. Callers must redact secrets before populating this.
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  protected AuditEvent() {
    // Required by JPA.
  }

  AuditEvent(
      UUID organizationId,
      UUID actorUserId,
      ActorType actorType,
      String actorLabel,
      String action,
      String resourceType,
      String resourceId,
      AuditOutcome outcome,
      String reason,
      String correlationId,
      String ipAddress,
      Map<String, Object> metadata,
      Instant occurredAt) {
    this.id = UUID.randomUUID();
    this.organizationId = organizationId;
    this.actorUserId = actorUserId;
    this.actorType = actorType;
    this.actorLabel = actorLabel;
    this.action = action;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.outcome = outcome;
    this.reason = reason;
    this.correlationId = correlationId;
    this.ipAddress = ipAddress;
    this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    this.occurredAt = occurredAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getActorUserId() {
    return actorUserId;
  }

  public ActorType getActorType() {
    return actorType;
  }

  public String getActorLabel() {
    return actorLabel;
  }

  public String getAction() {
    return action;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getResourceId() {
    return resourceId;
  }

  public AuditOutcome getOutcome() {
    return outcome;
  }

  public String getReason() {
    return reason;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public Map<String, Object> getMetadata() {
    return metadata == null ? Map.of() : Map.copyOf(metadata);
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof AuditEvent event && id != null && id.equals(event.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return "AuditEvent[action=%s, resource=%s/%s, outcome=%s]"
        .formatted(action, resourceType, resourceId, outcome);
  }
}
