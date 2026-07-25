package com.vaibhav.runbookos.organization;

import com.vaibhav.runbookos.identity.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Grants a user exactly one {@link Role} within one organization. This is the only object that
 * authorises tenant access; possession of an access token alone is never sufficient.
 */
@Entity
@Table(name = "memberships")
public class Membership {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "organization_id", nullable = false, updatable = false)
  private UUID organizationId;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private MembershipStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected Membership() {
    // Required by JPA.
  }

  private Membership(UUID id, UUID organizationId, UUID userId, Role role, Instant now) {
    this.id = id;
    this.organizationId = organizationId;
    this.userId = userId;
    this.role = role;
    this.status = MembershipStatus.ACTIVE;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static Membership create(UUID organizationId, UUID userId, Role role, Instant now) {
    return new Membership(UUID.randomUUID(), organizationId, userId, role, now);
  }

  public void changeRole(Role newRole, Instant at) {
    this.role = newRole;
    this.updatedAt = at;
  }

  public void suspend(Instant at) {
    this.status = MembershipStatus.SUSPENDED;
    this.updatedAt = at;
  }

  public void reactivate(Instant at) {
    this.status = MembershipStatus.ACTIVE;
    this.updatedAt = at;
  }

  public boolean isActive() {
    return status == MembershipStatus.ACTIVE;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getUserId() {
    return userId;
  }

  public Role getRole() {
    return role;
  }

  public MembershipStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof Membership membership && id != null && id.equals(membership.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return "Membership[id=%s, org=%s, role=%s, status=%s]"
        .formatted(id, organizationId, role, status);
  }
}
