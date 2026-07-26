package com.vaibhav.runbookos.runbook;

import com.vaibhav.runbookos.exception.ConflictException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "runbook_versions")
public class RunbookVersion {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "runbook_id", nullable = false)
  private UUID runbookId;

  @Column(name = "version_number", nullable = false)
  private int versionNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private RunbookVersionStatus status;

  @Column(name = "change_summary", length = 512)
  private String changeSummary;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "published_by")
  private UUID publishedBy;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Version
  @Column(name = "row_version")
  private long rowVersion;

  protected RunbookVersion() {}

  public static RunbookVersion draft(
      UUID org, UUID runbook, int number, String summary, UUID actor, Instant now) {
    RunbookVersion value = new RunbookVersion();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.runbookId = runbook;
    value.versionNumber = number;
    value.status = RunbookVersionStatus.DRAFT;
    value.changeSummary = summary;
    value.createdBy = actor;
    value.createdAt = now;
    return value;
  }

  public void publish(UUID actor, Instant now) {
    requireDraft();
    status = RunbookVersionStatus.PUBLISHED;
    publishedBy = actor;
    publishedAt = now;
  }

  public void supersede() {
    if (status == RunbookVersionStatus.PUBLISHED) status = RunbookVersionStatus.SUPERSEDED;
  }

  public void requireDraft() {
    if (status != RunbookVersionStatus.DRAFT)
      throw new ConflictException(
          "RUNBOOK_VERSION_IMMUTABLE", "Published runbook versions cannot be modified");
  }

  public UUID getId() {
    return id;
  }

  public UUID getRunbookId() {
    return runbookId;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public RunbookVersionStatus getStatus() {
    return status;
  }

  public String getChangeSummary() {
    return changeSummary;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }
}
