package com.vaibhav.runbookos.policy;

import com.vaibhav.runbookos.integration.IntegrationEnvironment;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "policies")
public class Policy {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private IntegrationEnvironment environment;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Policy() {}

  public static Policy create(
      UUID org, String name, IntegrationEnvironment environment, UUID actor, Instant now) {
    Policy value = new Policy();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.name = name;
    value.environment = environment;
    value.enabled = true;
    value.createdBy = actor;
    value.createdAt = now;
    value.updatedAt = now;
    return value;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public IntegrationEnvironment getEnvironment() {
    return environment;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }
}
