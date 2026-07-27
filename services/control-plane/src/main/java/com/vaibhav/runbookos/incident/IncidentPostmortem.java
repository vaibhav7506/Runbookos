package com.vaibhav.runbookos.incident;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "incident_postmortems")
public class IncidentPostmortem {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Column(name = "incident_id", nullable = false, updatable = false)
  private UUID incidentId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String summary;

  @Column(nullable = false, columnDefinition = "text")
  private String impact;

  @Column(name = "root_cause", nullable = false, columnDefinition = "text")
  private String rootCause;

  @Column(nullable = false, columnDefinition = "text")
  private String resolution;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "follow_up_actions", nullable = false, columnDefinition = "jsonb")
  private List<String> followUpActions;

  @Column(name = "generated_by", nullable = false, updatable = false)
  private UUID generatedBy;

  @Column(name = "generated_at", nullable = false, updatable = false)
  private Instant generatedAt;

  protected IncidentPostmortem() {}

  static IncidentPostmortem create(UUID org, Incident incident, UUID actor, Instant at) {
    IncidentPostmortem value = new IncidentPostmortem();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.incidentId = incident.getId();
    value.title = "Postmortem: " + incident.getTitle();
    value.summary =
        "A seeded deployment regression increased the error rate for "
            + incident.getAffectedService()
            + ". Human-governed remediation restored the service.";
    value.impact =
        "Checkout requests experienced elevated failures during the simulated incident window.";
    value.rootCause =
        "The demo deployment introduced a regression correlated with the collected commit and health evidence.";
    value.resolution =
        "Responders reviewed grounded AI analysis, approved a reversible simulated rollback, monitored recovery, and resolved the incident.";
    value.followUpActions =
        List.of(
            "Add a deployment health gate",
            "Alert on error-rate regression before full rollout",
            "Review rollback readiness during the next game day");
    value.generatedBy = actor;
    value.generatedAt = at;
    return value;
  }

  public UUID getId() {
    return id;
  }

  public UUID getIncidentId() {
    return incidentId;
  }

  public String getTitle() {
    return title;
  }

  public String getSummary() {
    return summary;
  }

  public String getImpact() {
    return impact;
  }

  public String getRootCause() {
    return rootCause;
  }

  public String getResolution() {
    return resolution;
  }

  public List<String> getFollowUpActions() {
    return List.copyOf(followUpActions);
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }
}
