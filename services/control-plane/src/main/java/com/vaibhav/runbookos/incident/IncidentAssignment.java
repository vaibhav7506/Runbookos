package com.vaibhav.runbookos.incident;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_assignments")
public class IncidentAssignment {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "incident_id", nullable = false)
  private UUID incidentId;

  @Column(name = "assignee_user_id", nullable = false)
  private UUID assigneeUserId;

  @Column(name = "assigned_by", nullable = false)
  private UUID assignedBy;

  @Column(name = "assigned_at", nullable = false)
  private Instant assignedAt;

  protected IncidentAssignment() {}

  public static IncidentAssignment create(
      UUID org, UUID incident, UUID assignee, UUID actor, Instant now) {
    IncidentAssignment v = new IncidentAssignment();
    v.id = UUID.randomUUID();
    v.organizationId = org;
    v.incidentId = incident;
    v.assigneeUserId = assignee;
    v.assignedBy = actor;
    v.assignedAt = now;
    return v;
  }
}
