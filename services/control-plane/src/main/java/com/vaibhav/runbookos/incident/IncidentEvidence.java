package com.vaibhav.runbookos.incident;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "incident_evidence")
public class IncidentEvidence {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "incident_id", nullable = false)
  private UUID incidentId;

  @Column(nullable = false, length = 48)
  private String kind;

  @Column(nullable = false, length = 240)
  private String title;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> content;

  @Column(length = 120)
  private String source;

  @Column(name = "collected_at", nullable = false)
  private Instant collectedAt;

  protected IncidentEvidence() {}

  public static IncidentEvidence create(
      UUID org,
      UUID incident,
      String kind,
      String title,
      Map<String, Object> content,
      String source,
      Instant now) {
    IncidentEvidence v = new IncidentEvidence();
    v.id = UUID.randomUUID();
    v.organizationId = org;
    v.incidentId = incident;
    v.kind = kind;
    v.title = title;
    v.content = Map.copyOf(content);
    v.source = source;
    v.collectedAt = now;
    return v;
  }

  public UUID getId() {
    return id;
  }

  public String getKind() {
    return kind;
  }

  public String getTitle() {
    return title;
  }

  public Map<String, Object> getContent() {
    return content;
  }

  public Instant getCollectedAt() {
    return collectedAt;
  }
}
