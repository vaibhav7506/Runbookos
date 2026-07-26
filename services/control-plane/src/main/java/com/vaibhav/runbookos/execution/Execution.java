package com.vaibhav.runbookos.execution;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "executions")
public class Execution {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "incident_id", nullable = false)
  private UUID incidentId;

  @Column(name = "workflow_key", nullable = false, length = 96)
  private String workflowKey;

  @Column(name = "workflow_version", nullable = false)
  private int workflowVersion;

  @Column(name = "idempotency_key", nullable = false, length = 255)
  private String idempotencyKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ExecutionStatus status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "allowed_action_ids", nullable = false, columnDefinition = "jsonb")
  private List<String> allowedActionIds;

  @Column(name = "token_nonce", nullable = false, length = 128)
  private String tokenNonce;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "timeout_at", nullable = false)
  private Instant timeoutAt;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Execution() {}

  public static Execution create(
      UUID org,
      UUID incident,
      String key,
      int workflowVersion,
      String idem,
      List<String> actions,
      String nonce,
      UUID actor,
      Instant now,
      Instant timeout) {
    Execution v = new Execution();
    v.id = UUID.randomUUID();
    v.organizationId = org;
    v.incidentId = incident;
    v.workflowKey = key;
    v.workflowVersion = workflowVersion;
    v.idempotencyKey = idem;
    v.allowedActionIds = List.copyOf(actions);
    v.tokenNonce = nonce;
    v.status = ExecutionStatus.QUEUED;
    v.createdBy = actor;
    v.createdAt = now;
    v.updatedAt = now;
    v.timeoutAt = timeout;
    return v;
  }

  public void transition(ExecutionStatus target, Instant now) {
    ExecutionStateMachine.require(status, target);
    status = target;
    updatedAt = now;
    if (target == ExecutionStatus.RUNNING && startedAt == null) startedAt = now;
    if (Set.of(
            ExecutionStatus.SUCCEEDED,
            ExecutionStatus.PARTIALLY_SUCCEEDED,
            ExecutionStatus.FAILED,
            ExecutionStatus.TIMED_OUT,
            ExecutionStatus.CANCELLED)
        .contains(target)) completedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getIncidentId() {
    return incidentId;
  }

  public String getWorkflowKey() {
    return workflowKey;
  }

  public int getWorkflowVersion() {
    return workflowVersion;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public List<String> getAllowedActionIds() {
    return allowedActionIds;
  }

  public String getTokenNonce() {
    return tokenNonce;
  }

  public Instant getTimeoutAt() {
    return timeoutAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
