package com.vaibhav.runbookos.execution;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "execution_events")
public class ExecutionEvent {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "execution_id", nullable = false)
  private UUID executionId;

  @Column(name = "step_execution_id")
  private UUID stepExecutionId;

  @Column(name = "event_type", nullable = false, length = 64)
  private String eventType;

  @Column(length = 32)
  private String status;

  @Column(nullable = false, length = 512)
  private String message;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> details;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected ExecutionEvent() {}

  public static ExecutionEvent create(
      UUID org,
      UUID execution,
      UUID step,
      String type,
      String status,
      String message,
      Map<String, Object> details,
      Instant now) {
    ExecutionEvent v = new ExecutionEvent();
    v.id = UUID.randomUUID();
    v.organizationId = org;
    v.executionId = execution;
    v.stepExecutionId = step;
    v.eventType = type;
    v.status = status;
    v.message = message;
    v.details = details == null ? Map.of() : Map.copyOf(details);
    v.occurredAt = now;
    return v;
  }

  public UUID getId() {
    return id;
  }

  public UUID getStepExecutionId() {
    return stepExecutionId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public Map<String, Object> getDetails() {
    return details;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
