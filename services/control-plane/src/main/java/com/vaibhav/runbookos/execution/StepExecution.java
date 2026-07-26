package com.vaibhav.runbookos.execution;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "step_executions")
public class StepExecution {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "execution_id", nullable = false)
  private UUID executionId;

  @Column(name = "action_id", nullable = false, length = 96)
  private String actionId;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(name = "sequence_number", nullable = false)
  private int sequenceNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private StepStatus status;

  @Column(nullable = false)
  private int attempt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "error_code", length = 96)
  private String errorCode;

  @Column(name = "error_message", length = 512)
  private String errorMessage;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> output = Map.of();

  @Version private long version;

  protected StepExecution() {}

  public static StepExecution queued(
      UUID org, UUID execution, String action, String name, int sequence, int attempt) {
    StepExecution v = new StepExecution();
    v.id = UUID.randomUUID();
    v.organizationId = org;
    v.executionId = execution;
    v.actionId = action;
    v.name = name;
    v.sequenceNumber = sequence;
    v.attempt = attempt;
    v.status = StepStatus.QUEUED;
    return v;
  }

  public void update(
      StepStatus target,
      Map<String, Object> output,
      String errorCode,
      String errorMessage,
      Instant now) {
    status = target;
    if (target == StepStatus.RUNNING && startedAt == null) startedAt = now;
    if (Set.of(StepStatus.SUCCEEDED, StepStatus.FAILED, StepStatus.SKIPPED, StepStatus.DENIED)
        .contains(target)) completedAt = now;
    this.output = output == null ? Map.of() : Map.copyOf(output);
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  public UUID getId() {
    return id;
  }

  public String getActionId() {
    return actionId;
  }

  public String getName() {
    return name;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public StepStatus getStatus() {
    return status;
  }

  public int getAttempt() {
    return attempt;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Map<String, Object> getOutput() {
    return output;
  }
}
