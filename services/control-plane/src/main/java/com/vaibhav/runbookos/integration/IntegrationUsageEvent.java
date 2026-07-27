package com.vaibhav.runbookos.integration;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_usage_events")
public class IntegrationUsageEvent {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Column(name = "integration_id", nullable = false, updatable = false)
  private UUID integrationId;

  @Column(nullable = false, length = 80)
  private String operation;

  @Column(nullable = false, length = 24)
  private String outcome;

  @Column(length = 512)
  private String detail;

  @Column(name = "correlation_id", length = 128)
  private String correlationId;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  protected IntegrationUsageEvent() {}

  static IntegrationUsageEvent create(
      UUID org,
      UUID integrationId,
      String operation,
      String outcome,
      String detail,
      String correlationId,
      Instant at) {
    IntegrationUsageEvent value = new IntegrationUsageEvent();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.integrationId = integrationId;
    value.operation = operation;
    value.outcome = outcome;
    value.detail = detail;
    value.correlationId = correlationId;
    value.occurredAt = at;
    return value;
  }

  public String getOperation() {
    return operation;
  }

  public String getOutcome() {
    return outcome;
  }

  public String getDetail() {
    return detail;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
