package com.vaibhav.runbookos.policy;

import com.vaibhav.runbookos.integration.IntegrationEnvironment;
import com.vaibhav.runbookos.runbook.RiskClassification;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "policy_decision_logs")
public class PolicyDecisionLog {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "policy_id")
  private UUID policyId;

  @Column(name = "incident_id")
  private UUID incidentId;

  @Column(name = "runbook_version_id")
  private UUID runbookVersionId;

  @Column(name = "step_key", nullable = false, length = 80)
  private String stepKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private IntegrationEnvironment environment;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_classification", nullable = false, length = 24)
  private RiskClassification riskClassification;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PolicyDecision decision;

  @Column(nullable = false, columnDefinition = "text")
  private String reason;

  @Column(name = "actor_user_id", nullable = false)
  private UUID actorUserId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected PolicyDecisionLog() {}

  public static PolicyDecisionLog create(
      UUID org,
      UUID policy,
      UUID incident,
      UUID runbookVersion,
      String step,
      IntegrationEnvironment environment,
      RiskClassification risk,
      PolicyDecision decision,
      String reason,
      UUID actor,
      Instant now) {
    PolicyDecisionLog value = new PolicyDecisionLog();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.policyId = policy;
    value.incidentId = incident;
    value.runbookVersionId = runbookVersion;
    value.stepKey = step;
    value.environment = environment;
    value.riskClassification = risk;
    value.decision = decision;
    value.reason = reason;
    value.actorUserId = actor;
    value.createdAt = now;
    return value;
  }
}
