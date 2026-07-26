package com.vaibhav.runbookos.analysis;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_provider_failures")
public class AiProviderFailure {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AiProvider provider;

  @Column(name = "failure_count", nullable = false)
  private int failureCount;

  @Column(name = "circuit_open_until")
  private Instant circuitOpenUntil;

  @Column(name = "last_failure_reason", length = 512)
  private String lastFailureReason;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected AiProviderFailure() {}

  public static AiProviderFailure create(UUID org, AiProvider provider, Instant now) {
    AiProviderFailure value = new AiProviderFailure();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.provider = provider;
    value.updatedAt = now;
    return value;
  }

  public boolean isOpen(Instant now) {
    return circuitOpenUntil != null && circuitOpenUntil.isAfter(now);
  }

  public void failed(String reason, Instant now) {
    failureCount++;
    lastFailureReason =
        reason == null ? "provider failure" : reason.substring(0, Math.min(512, reason.length()));
    if (failureCount >= 3) circuitOpenUntil = now.plusSeconds(60);
    updatedAt = now;
  }

  public void succeeded(Instant now) {
    failureCount = 0;
    circuitOpenUntil = null;
    lastFailureReason = null;
    updatedAt = now;
  }
}
