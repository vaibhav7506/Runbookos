package com.vaibhav.runbookos.policy;

import com.vaibhav.runbookos.audit.*;
import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.*;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.integration.IntegrationEnvironment;
import com.vaibhav.runbookos.organization.*;
import com.vaibhav.runbookos.runbook.RiskClassification;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyEngine {
  private final PolicyRepository policies;
  private final PolicyRuleRepository rules;
  private final PolicyDecisionLogRepository logs;
  private final OrganizationRepository organizations;
  private final TenantAccessService access;
  private final AuditService audit;
  private final TimeProvider time;

  public PolicyEngine(
      PolicyRepository policies,
      PolicyRuleRepository rules,
      PolicyDecisionLogRepository logs,
      OrganizationRepository organizations,
      TenantAccessService access,
      AuditService audit,
      TimeProvider time) {
    this.policies = policies;
    this.rules = rules;
    this.logs = logs;
    this.organizations = organizations;
    this.access = access;
    this.audit = audit;
    this.time = time;
  }

  @Transactional
  public DecisionResult decide(
      UUID org,
      UUID incident,
      UUID runbookVersion,
      String stepKey,
      IntegrationEnvironment environment,
      RiskClassification risk,
      Role requiredRole,
      UUID actor) {
    Role actorRole = access.require(org, actor).getRole();
    ensureDefaults(org, actor);
    Policy policy =
        policies
            .findByOrganizationIdAndEnvironmentAndEnabledTrue(org, environment)
            .orElseThrow(() -> new ResourceNotFoundException("policy", environment.name()));
    PolicyRule rule =
        rules
            .findByPolicyIdAndRiskClassification(policy.getId(), risk)
            .orElseThrow(() -> new ResourceNotFoundException("policy rule", risk.name()));
    boolean demoMode =
        organizations
            .findById(org)
            .orElseThrow(() -> new ResourceNotFoundException("organization", org))
            .isDemoMode();
    DecisionResult result = evaluate(rule, actorRole, requiredRole, risk, demoMode);
    logs.save(
        PolicyDecisionLog.create(
            org,
            policy.getId(),
            incident,
            runbookVersion,
            stepKey,
            environment,
            risk,
            result.decision(),
            result.reason(),
            actor,
            time.nowTruncated()));
    audit.record(
        AuditRecord.builder(AuditActions.POLICY_DECIDED, "policy_decision")
            .organization(org)
            .actor(actor, null)
            .metadata("stepKey", stepKey)
            .metadata("decision", result.decision())
            .metadata("reason", result.reason())
            .build());
    return result.withPolicy(policy.getId());
  }

  static DecisionResult evaluate(
      PolicyRule rule,
      Role actorRole,
      Role requiredRole,
      RiskClassification risk,
      boolean demoMode) {
    if (risk == RiskClassification.PROHIBITED)
      return DecisionResult.denied("PROHIBITED actions are never permitted");
    if (risk == RiskClassification.HIGH_RISK && demoMode)
      return DecisionResult.denied("HIGH_RISK actions are disabled in Demo Mode");
    if (!roleSatisfies(actorRole, requiredRole))
      return DecisionResult.denied(
          "Actor role " + actorRole + " does not satisfy required role " + requiredRole);
    return new DecisionResult(
        null,
        rule.getDecision(),
        switch (rule.getDecision()) {
          case ALLOW -> "Policy permits this validated read-only action";
          case REQUIRE_APPROVAL -> "Policy requires one eligible human approval";
          case REQUIRE_ELEVATED_APPROVAL -> "Policy requires two distinct eligible human approvals";
          case DENY -> "Policy denies this action in the selected environment";
        },
        rule.getRequiredApprovals(),
        rule.getApproverRoles(),
        rule.isTypedConfirmationRequired(),
        rule.getExpirationMinutes());
  }

  private static boolean roleSatisfies(Role actor, Role required) {
    if (required == Role.RESPONDER) return actor.canRespond();
    if (required == Role.ADMIN) return actor == Role.OWNER || actor == Role.ADMIN;
    if (required == Role.OWNER) return actor == Role.OWNER;
    if (required == Role.VIEWER) return true;
    return actor.canReadAudit();
  }

  @Transactional
  public List<PolicyView> list(UUID org, UUID actor) {
    access.require(org, actor);
    ensureDefaults(org, actor);
    return policies.findByOrganizationIdOrderByEnvironmentAsc(org).stream()
        .map(
            policy ->
                PolicyView.from(
                    policy, rules.findByPolicyIdOrderByRiskClassificationAsc(policy.getId())))
        .toList();
  }

  @Transactional
  public PolicyView updateRule(
      UUID org, UUID policyId, RiskClassification risk, UpdateRule request, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    Policy policy =
        policies
            .findByIdAndOrganizationId(policyId, org)
            .orElseThrow(() -> new ResourceNotFoundException("policy", policyId));
    PolicyRule rule =
        rules
            .findByPolicyIdAndRiskClassification(policyId, risk)
            .orElseThrow(() -> new ResourceNotFoundException("policy rule", risk.name()));
    if (risk == RiskClassification.PROHIBITED && request.decision() != PolicyDecision.DENY)
      throw new ConflictException(
          "PROHIBITED_POLICY_IMMUTABLE", "PROHIBITED actions must always be denied");
    if (risk == RiskClassification.HIGH_RISK
        && (request.decision() != PolicyDecision.REQUIRE_ELEVATED_APPROVAL
            || request.requiredApprovals() < 2
            || !request.typedConfirmationRequired()))
      throw new ConflictException(
          "HIGH_RISK_POLICY_TOO_WEAK",
          "HIGH_RISK actions require elevated approval, two users, and typed confirmation");
    rule.update(
        request.decision(),
        request.requiredApprovals(),
        request.approverRoles(),
        request.typedConfirmationRequired(),
        request.expirationMinutes());
    return PolicyView.from(
        policy, rules.findByPolicyIdOrderByRiskClassificationAsc(policy.getId()));
  }

  private void ensureDefaults(UUID org, UUID actor) {
    for (IntegrationEnvironment environment : IntegrationEnvironment.values()) {
      if (policies.findByOrganizationIdAndEnvironmentAndEnabledTrue(org, environment).isPresent())
        continue;
      Policy policy =
          policies.save(
              Policy.create(
                  org,
                  environment.name().charAt(0)
                      + environment.name().substring(1).toLowerCase(Locale.ROOT)
                      + " default",
                  environment,
                  actor,
                  time.nowTruncated()));
      List<Role> eligible = List.of(Role.OWNER, Role.ADMIN, Role.RESPONDER);
      rules.save(
          PolicyRule.create(
              org,
              policy.getId(),
              RiskClassification.READ_ONLY,
              PolicyDecision.ALLOW,
              0,
              eligible,
              false,
              30));
      rules.save(
          PolicyRule.create(
              org,
              policy.getId(),
              RiskClassification.REVERSIBLE,
              PolicyDecision.REQUIRE_APPROVAL,
              1,
              eligible,
              false,
              30));
      rules.save(
          PolicyRule.create(
              org,
              policy.getId(),
              RiskClassification.HIGH_RISK,
              PolicyDecision.REQUIRE_ELEVATED_APPROVAL,
              2,
              List.of(Role.OWNER, Role.ADMIN),
              true,
              15));
      rules.save(
          PolicyRule.create(
              org,
              policy.getId(),
              RiskClassification.PROHIBITED,
              PolicyDecision.DENY,
              0,
              List.of(Role.OWNER),
              false,
              5));
    }
  }

  public record DecisionResult(
      UUID policyId,
      PolicyDecision decision,
      String reason,
      int requiredApprovals,
      List<Role> approverRoles,
      boolean typedConfirmationRequired,
      int expirationMinutes) {
    public DecisionResult {
      approverRoles = List.copyOf(approverRoles);
    }

    static DecisionResult denied(String reason) {
      return new DecisionResult(null, PolicyDecision.DENY, reason, 0, List.of(), false, 5);
    }

    DecisionResult withPolicy(UUID value) {
      return new DecisionResult(
          value,
          decision,
          reason,
          requiredApprovals,
          approverRoles,
          typedConfirmationRequired,
          expirationMinutes);
    }
  }

  public record UpdateRule(
      PolicyDecision decision,
      int requiredApprovals,
      List<Role> approverRoles,
      boolean typedConfirmationRequired,
      int expirationMinutes) {}

  public record RuleView(
      RiskClassification risk,
      PolicyDecision decision,
      int requiredApprovals,
      List<Role> approverRoles,
      boolean typedConfirmationRequired,
      int expirationMinutes) {
    static RuleView from(PolicyRule rule) {
      return new RuleView(
          rule.getRiskClassification(),
          rule.getDecision(),
          rule.getRequiredApprovals(),
          rule.getApproverRoles(),
          rule.isTypedConfirmationRequired(),
          rule.getExpirationMinutes());
    }
  }

  public record PolicyView(
      UUID id,
      String name,
      IntegrationEnvironment environment,
      boolean enabled,
      List<RuleView> rules) {
    static PolicyView from(Policy policy, List<PolicyRule> rules) {
      return new PolicyView(
          policy.getId(),
          policy.getName(),
          policy.getEnvironment(),
          policy.isEnabled(),
          rules.stream().map(RuleView::from).toList());
    }
  }
}
