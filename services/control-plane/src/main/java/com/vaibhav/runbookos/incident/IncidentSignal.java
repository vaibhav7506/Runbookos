package com.vaibhav.runbookos.incident;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "incident_signals")
public class IncidentSignal {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Column(name = "incident_id", nullable = false, updatable = false)
  private UUID incidentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private SignalSource source;

  @Column(name = "external_id", length = 255)
  private String externalId;

  @Column(nullable = false, length = 64)
  private String fingerprint;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload;

  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  protected IncidentSignal() {}

  public static IncidentSignal create(
      UUID org,
      UUID incident,
      SignalSource source,
      String externalId,
      String fingerprint,
      Map<String, Object> payload,
      Instant now) {
    IncidentSignal value = new IncidentSignal();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.incidentId = incident;
    value.source = source;
    value.externalId = externalId;
    value.fingerprint = fingerprint;
    value.payload = Map.copyOf(payload);
    value.receivedAt = now;
    return value;
  }

  public UUID getId() {
    return id;
  }

  public UUID getIncidentId() {
    return incidentId;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }
}
