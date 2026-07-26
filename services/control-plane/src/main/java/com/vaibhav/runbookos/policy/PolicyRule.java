package com.vaibhav.runbookos.policy;

import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.runbook.RiskClassification;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "policy_rules")
public class PolicyRule {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "policy_id", nullable = false)
  private UUID policyId;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_classification", nullable = false, length = 24)
  private RiskClassification riskClassification;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PolicyDecision decision;

  @Column(name = "required_approvals", nullable = false)
  private int requiredApprovals;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "approver_roles", nullable = false, columnDefinition = "jsonb")
  private List<String> approverRoles;

  @Column(name = "typed_confirmation_required", nullable = false)
  private boolean typedConfirmationRequired;

  @Column(name = "expiration_minutes", nullable = false)
  private int expirationMinutes;

  protected PolicyRule() {}

  public static PolicyRule create(
      UUID org,
      UUID policy,
      RiskClassification risk,
      PolicyDecision decision,
      int approvals,
      List<Role> roles,
      boolean typed,
      int expiry) {
    PolicyRule value = new PolicyRule();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.policyId = policy;
    value.riskClassification = risk;
    value.decision = decision;
    value.requiredApprovals = approvals;
    value.approverRoles = roles.stream().map(Enum::name).toList();
    value.typedConfirmationRequired = typed;
    value.expirationMinutes = expiry;
    return value;
  }

  public UUID getPolicyId() {
    return policyId;
  }

  public RiskClassification getRiskClassification() {
    return riskClassification;
  }

  public PolicyDecision getDecision() {
    return decision;
  }

  public int getRequiredApprovals() {
    return requiredApprovals;
  }

  public List<Role> getApproverRoles() {
    return approverRoles.stream().map(Role::valueOf).toList();
  }

  public boolean isTypedConfirmationRequired() {
    return typedConfirmationRequired;
  }

  public int getExpirationMinutes() {
    return expirationMinutes;
  }

  public void update(
      PolicyDecision newDecision,
      int approvals,
      List<Role> roles,
      boolean typedConfirmation,
      int expiryMinutes) {
    decision = newDecision;
    requiredApprovals = approvals;
    approverRoles = roles.stream().map(Enum::name).toList();
    typedConfirmationRequired = typedConfirmation;
    expirationMinutes = expiryMinutes;
  }
}
