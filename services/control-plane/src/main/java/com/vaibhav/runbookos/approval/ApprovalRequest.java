package com.vaibhav.runbookos.approval;

import com.vaibhav.runbookos.integration.IntegrationEnvironment;
import com.vaibhav.runbookos.policy.PolicyDecision;
import com.vaibhav.runbookos.runbook.RiskClassification;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_requests")
public class ApprovalRequest {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "incident_id")
  private UUID incidentId;

  @Column(name = "execution_id")
  private UUID executionId;

  @Column(name = "runbook_version_id")
  private UUID runbookVersionId;

  @Column(name = "step_key", nullable = false, length = 80)
  private String stepKey;

  @Column(name = "action_name", nullable = false, length = 160)
  private String actionName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private IntegrationEnvironment environment;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_classification", nullable = false, length = 24)
  private RiskClassification riskClassification;

  @Enumerated(EnumType.STRING)
  @Column(name = "policy_decision", nullable = false, length = 32)
  private PolicyDecision policyDecision;

  @Column(name = "policy_reason", nullable = false, columnDefinition = "text")
  private String policyReason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private ApprovalStatus status;

  @Column(name = "required_approvals", nullable = false)
  private int requiredApprovals;

  @Column(name = "confirmation_phrase", length = 160)
  private String confirmationPhrase;

  @Column(name = "requested_by", nullable = false)
  private UUID requestedBy;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "cancellation_reason", length = 512)
  private String cancellationReason;

  @Version private long version;

  protected ApprovalRequest() {}

  public static ApprovalRequest pending(
      UUID org,
      UUID incident,
      UUID execution,
      UUID runbookVersion,
      String stepKey,
      String actionName,
      IntegrationEnvironment environment,
      RiskClassification risk,
      PolicyDecision decision,
      String reason,
      int requiredApprovals,
      String phrase,
      UUID actor,
      Instant now,
      Instant expiresAt) {
    ApprovalRequest value = new ApprovalRequest();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.incidentId = incident;
    value.executionId = execution;
    value.runbookVersionId = runbookVersion;
    value.stepKey = stepKey;
    value.actionName = actionName;
    value.environment = environment;
    value.riskClassification = risk;
    value.policyDecision = decision;
    value.policyReason = reason;
    value.status = ApprovalStatus.PENDING;
    value.requiredApprovals = requiredApprovals;
    value.confirmationPhrase = phrase;
    value.requestedBy = actor;
    value.requestedAt = now;
    value.expiresAt = expiresAt;
    return value;
  }

  public void approve(Instant now) {
    status = ApprovalStatus.APPROVED;
    resolvedAt = now;
  }

  public void deny(Instant now) {
    status = ApprovalStatus.DENIED;
    resolvedAt = now;
  }

  public void expire(Instant now) {
    if (status == ApprovalStatus.PENDING) {
      status = ApprovalStatus.EXPIRED;
      resolvedAt = now;
    }
  }

  public void cancel(String reason, Instant now) {
    if (status == ApprovalStatus.PENDING) {
      status = ApprovalStatus.CANCELLED;
      cancellationReason = reason;
      resolvedAt = now;
    }
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getIncidentId() {
    return incidentId;
  }

  public UUID getExecutionId() {
    return executionId;
  }

  public UUID getRunbookVersionId() {
    return runbookVersionId;
  }

  public String getStepKey() {
    return stepKey;
  }

  public String getActionName() {
    return actionName;
  }

  public IntegrationEnvironment getEnvironment() {
    return environment;
  }

  public RiskClassification getRiskClassification() {
    return riskClassification;
  }

  public PolicyDecision getPolicyDecision() {
    return policyDecision;
  }

  public String getPolicyReason() {
    return policyReason;
  }

  public ApprovalStatus getStatus() {
    return status;
  }

  public int getRequiredApprovals() {
    return requiredApprovals;
  }

  public String getConfirmationPhrase() {
    return confirmationPhrase;
  }

  public UUID getRequestedBy() {
    return requestedBy;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }
}
