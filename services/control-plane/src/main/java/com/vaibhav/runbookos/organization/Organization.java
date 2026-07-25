package com.vaibhav.runbookos.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * The tenant boundary. Every organization-owned record carries this id, and every repository query
 * for such records filters on it.
 */
@Entity
@Table(name = "organizations")
public class Organization {

  private static final int SLUG_MAX_LENGTH = 64;

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 64)
  private String slug;

  /** When true, the policy engine refuses HIGH_RISK actions for this tenant. */
  @Column(name = "demo_mode", nullable = false)
  private boolean demoMode;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected Organization() {
    // Required by JPA.
  }

  private Organization(
      UUID id, String name, String slug, boolean demoMode, UUID createdBy, Instant now) {
    this.id = id;
    this.name = name;
    this.slug = slug;
    this.demoMode = demoMode;
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static Organization create(
      String name, String slug, boolean demoMode, UUID createdBy, Instant now) {
    return new Organization(UUID.randomUUID(), name, slug, demoMode, createdBy, now);
  }

  /**
   * Derives a URL-safe slug from a display name. Callers must still resolve collisions, since
   * distinct names can produce the same slug.
   */
  public static String slugify(String name) {
    String slug =
        name.trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
    if (slug.isEmpty()) {
      slug = "org";
    }
    return slug.length() > SLUG_MAX_LENGTH ? slug.substring(0, SLUG_MAX_LENGTH) : slug;
  }

  public void rename(String newName, Instant at) {
    this.name = newName;
    this.updatedAt = at;
  }

  public void setDemoMode(boolean enabled, Instant at) {
    this.demoMode = enabled;
    this.updatedAt = at;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getSlug() {
    return slug;
  }

  public boolean isDemoMode() {
    return demoMode;
  }

  public UUID getCreatedBy() {
    return createdBy;
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
    return other instanceof Organization org && id != null && id.equals(org.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return "Organization[id=%s, slug=%s]".formatted(id, slug);
  }
}
