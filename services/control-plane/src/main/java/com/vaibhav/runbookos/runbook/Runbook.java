package com.vaibhav.runbookos.runbook;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "runbooks")
public class Runbook {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Runbook() {}

  public static Runbook create(UUID org, String name, String description, UUID actor, Instant now) {
    Runbook value = new Runbook();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.name = name;
    value.description = description;
    value.createdBy = actor;
    value.createdAt = now;
    value.updatedAt = now;
    return value;
  }

  public void touch(Instant now) {
    updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
