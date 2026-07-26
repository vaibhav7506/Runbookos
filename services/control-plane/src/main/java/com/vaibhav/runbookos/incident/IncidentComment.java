package com.vaibhav.runbookos.incident;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_comments")
public class IncidentComment {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "incident_id", nullable = false)
  private UUID incidentId;

  @Column(name = "author_user_id", nullable = false)
  private UUID authorUserId;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected IncidentComment() {}

  public static IncidentComment create(
      UUID org, UUID incident, UUID author, String body, Instant now) {
    IncidentComment v = new IncidentComment();
    v.id = UUID.randomUUID();
    v.organizationId = org;
    v.incidentId = incident;
    v.authorUserId = author;
    v.body = body;
    v.createdAt = now;
    v.updatedAt = now;
    return v;
  }

  public UUID getId() {
    return id;
  }

  public UUID getAuthorUserId() {
    return authorUserId;
  }

  public String getBody() {
    return body;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
