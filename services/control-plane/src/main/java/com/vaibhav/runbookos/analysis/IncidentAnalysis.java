package com.vaibhav.runbookos.analysis;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "incident_analyses")
public class IncidentAnalysis {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "incident_id", nullable = false)
  private UUID incidentId;

  @Column(nullable = false, length = 24)
  private String status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AiProvider provider;

  @Column(nullable = false, length = 120)
  private String model;

  @Column(name = "prompt_key", nullable = false, length = 80)
  private String promptKey;

  @Column(name = "prompt_version", nullable = false)
  private int promptVersion;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> output;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> quality;

  @Column(name = "latency_ms", nullable = false)
  private long latencyMs;

  @Column(name = "input_tokens", nullable = false)
  private int inputTokens;

  @Column(name = "output_tokens", nullable = false)
  private int outputTokens;

  @Column(name = "estimated_cost_usd", nullable = false, precision = 12, scale = 6)
  private BigDecimal estimatedCostUsd;

  @Column(name = "fallback_reason", length = 512)
  private String fallbackReason;

  @Column(name = "requested_by", nullable = false)
  private UUID requestedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected IncidentAnalysis() {}

  public static IncidentAnalysis completed(
      UUID org,
      UUID incident,
      AiProvider provider,
      String model,
      Map<String, Object> output,
      Map<String, Object> quality,
      long latency,
      int inputTokens,
      int outputTokens,
      BigDecimal cost,
      String fallbackReason,
      UUID actor,
      Instant now) {
    IncidentAnalysis value = new IncidentAnalysis();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.incidentId = incident;
    value.status = "COMPLETED";
    value.provider = provider;
    value.model = model;
    value.promptKey = "incident-analysis";
    value.promptVersion = 1;
    value.output = Map.copyOf(output);
    value.quality = Map.copyOf(quality);
    value.latencyMs = latency;
    value.inputTokens = inputTokens;
    value.outputTokens = outputTokens;
    value.estimatedCostUsd = cost;
    value.fallbackReason = fallbackReason;
    value.requestedBy = actor;
    value.createdAt = now;
    return value;
  }

  public UUID getId() {
    return id;
  }

  public UUID getIncidentId() {
    return incidentId;
  }

  public AiProvider getProvider() {
    return provider;
  }

  public String getModel() {
    return model;
  }

  public int getPromptVersion() {
    return promptVersion;
  }

  public Map<String, Object> getOutput() {
    return Map.copyOf(output);
  }

  public Map<String, Object> getQuality() {
    return Map.copyOf(quality);
  }

  public long getLatencyMs() {
    return latencyMs;
  }

  public int getInputTokens() {
    return inputTokens;
  }

  public int getOutputTokens() {
    return outputTokens;
  }

  public BigDecimal getEstimatedCostUsd() {
    return estimatedCostUsd;
  }

  public String getFallbackReason() {
    return fallbackReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
