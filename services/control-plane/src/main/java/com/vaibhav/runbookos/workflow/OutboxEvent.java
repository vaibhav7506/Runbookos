package com.vaibhav.runbookos.workflow;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "aggregate_type", nullable = false, length = 64)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Column(name = "event_type", nullable = false, length = 96)
  private String eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private OutboxStatus status;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @Column(name = "claimed_at")
  private Instant claimedAt;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  @Column(name = "last_error", length = 512)
  private String lastError;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected OutboxEvent() {}

  public static OutboxEvent create(
      UUID org,
      String aggregateType,
      UUID aggregateId,
      String type,
      Map<String, Object> payload,
      Instant now) {
    OutboxEvent v = new OutboxEvent();
    v.id = UUID.randomUUID();
    v.organizationId = org;
    v.aggregateType = aggregateType;
    v.aggregateId = aggregateId;
    v.eventType = type;
    v.payload = Map.copyOf(payload);
    v.status = OutboxStatus.PENDING;
    v.nextAttemptAt = now;
    v.createdAt = now;
    return v;
  }

  public void claim(Instant now) {
    status = OutboxStatus.PROCESSING;
    claimedAt = now;
    attempts++;
  }

  public void delivered(Instant now) {
    status = OutboxStatus.DELIVERED;
    deliveredAt = now;
    lastError = null;
  }

  public void failed(String error, Instant now) {
    lastError =
        error == null ? "dispatch failed" : error.substring(0, Math.min(512, error.length()));
    if (attempts >= 5) status = OutboxStatus.DEAD;
    else {
      status = OutboxStatus.PENDING;
      nextAttemptAt = now.plusSeconds(Math.min(300, 1L << attempts));
    }
  }

  public void redrive(Instant now) {
    if (status != OutboxStatus.DEAD) {
      throw new IllegalStateException("Only dead-letter events can be redriven");
    }
    status = OutboxStatus.PENDING;
    attempts = 0;
    nextAttemptAt = now;
    claimedAt = null;
    deliveredAt = null;
    lastError = null;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getAggregateId() {
    return aggregateId;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public String getEventType() {
    return eventType;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public int getAttempts() {
    return attempts;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
