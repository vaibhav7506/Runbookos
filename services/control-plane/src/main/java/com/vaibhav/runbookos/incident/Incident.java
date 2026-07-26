package com.vaibhav.runbookos.incident;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Column(nullable = false, length = 240)
  private String title;

  @Column(columnDefinition = "text")
  private String summary;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private IncidentStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 8)
  private IncidentSeverity severity;

  @Column(nullable = false)
  private int priority;

  @Column(name = "affected_service", nullable = false, length = 120)
  private String affectedService;

  @Column(nullable = false, length = 64)
  private String fingerprint;

  @Column(name = "assignee_user_id")
  private UUID assigneeUserId;

  @Column(name = "signal_count", nullable = false)
  private int signalCount;

  @Column(name = "first_detected_at", nullable = false, updatable = false)
  private Instant firstDetectedAt;

  @Column(name = "last_signal_at", nullable = false)
  private Instant lastSignalAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Incident() {}

  private Incident(
      UUID organizationId,
      String title,
      String summary,
      IncidentSeverity severity,
      int priority,
      String service,
      String fingerprint,
      Instant now) {
    this.id = UUID.randomUUID();
    this.organizationId = organizationId;
    this.title = title;
    this.summary = summary;
    this.status = IncidentStatus.DETECTED;
    this.severity = severity;
    this.priority = priority;
    this.affectedService = service;
    this.fingerprint = fingerprint;
    this.signalCount = 1;
    this.firstDetectedAt = now;
    this.lastSignalAt = now;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static Incident create(
      UUID organizationId,
      String title,
      String summary,
      IncidentSeverity severity,
      int priority,
      String service,
      String fingerprint,
      Instant now) {
    return new Incident(
        organizationId, title, summary, severity, priority, service, fingerprint, now);
  }

  public void recordSignal(Instant at) {
    signalCount++;
    lastSignalAt = at;
    updatedAt = at;
  }

  public void transition(IncidentStatus target, Instant at) {
    IncidentStateMachine.requireAllowed(status, target);
    status = target;
    updatedAt = at;
    if (target == IncidentStatus.RESOLVED) resolvedAt = at;
  }

  public void assign(UUID userId, Instant at) {
    assigneeUserId = userId;
    updatedAt = at;
  }

  public void setSeverity(IncidentSeverity value, Instant at) {
    severity = value;
    updatedAt = at;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getTitle() {
    return title;
  }

  public String getSummary() {
    return summary;
  }

  public IncidentStatus getStatus() {
    return status;
  }

  public IncidentSeverity getSeverity() {
    return severity;
  }

  public int getPriority() {
    return priority;
  }

  public String getAffectedService() {
    return affectedService;
  }

  public String getFingerprint() {
    return fingerprint;
  }

  public UUID getAssigneeUserId() {
    return assigneeUserId;
  }

  public int getSignalCount() {
    return signalCount;
  }

  public Instant getFirstDetectedAt() {
    return firstDetectedAt;
  }

  public Instant getLastSignalAt() {
    return lastSignalAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
