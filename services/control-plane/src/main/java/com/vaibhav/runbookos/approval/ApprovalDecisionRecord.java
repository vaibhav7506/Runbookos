package com.vaibhav.runbookos.approval;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_decisions")
public class ApprovalDecisionRecord {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "approval_request_id", nullable = false)
  private UUID approvalRequestId;

  @Column(name = "approver_user_id", nullable = false)
  private UUID approverUserId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ApprovalChoice decision;

  @Column(length = 1000)
  private String reason;

  @Column(name = "decided_at", nullable = false)
  private Instant decidedAt;

  protected ApprovalDecisionRecord() {}

  public static ApprovalDecisionRecord create(
      UUID org, UUID request, UUID approver, ApprovalChoice choice, String reason, Instant now) {
    ApprovalDecisionRecord value = new ApprovalDecisionRecord();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.approvalRequestId = request;
    value.approverUserId = approver;
    value.decision = choice;
    value.reason = reason;
    value.decidedAt = now;
    return value;
  }

  public UUID getApproverUserId() {
    return approverUserId;
  }

  public ApprovalChoice getDecision() {
    return decision;
  }

  public String getReason() {
    return reason;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }
}
