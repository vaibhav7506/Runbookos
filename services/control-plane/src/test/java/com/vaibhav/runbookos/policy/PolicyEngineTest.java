package com.vaibhav.runbookos.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.runbook.RiskClassification;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PolicyEngineTest {
  private final UUID org = UUID.randomUUID();
  private final UUID policy = UUID.randomUUID();

  @Test
  void readOnlyAllowsAndReversibleRequiresApproval() {
    assertThat(
            decide(RiskClassification.READ_ONLY, PolicyDecision.ALLOW, 0, Role.RESPONDER, false)
                .decision())
        .isEqualTo(PolicyDecision.ALLOW);
    assertThat(
            decide(
                    RiskClassification.REVERSIBLE,
                    PolicyDecision.REQUIRE_APPROVAL,
                    1,
                    Role.RESPONDER,
                    false)
                .decision())
        .isEqualTo(PolicyDecision.REQUIRE_APPROVAL);
  }

  @Test
  void highRiskNeedsElevatedApprovalAndIsDeniedInDemo() {
    var result =
        decide(
            RiskClassification.HIGH_RISK,
            PolicyDecision.REQUIRE_ELEVATED_APPROVAL,
            2,
            Role.ADMIN,
            false);
    assertThat(result.decision()).isEqualTo(PolicyDecision.REQUIRE_ELEVATED_APPROVAL);
    assertThat(result.requiredApprovals()).isEqualTo(2);
    assertThat(
            decide(
                    RiskClassification.HIGH_RISK,
                    PolicyDecision.REQUIRE_ELEVATED_APPROVAL,
                    2,
                    Role.ADMIN,
                    true)
                .decision())
        .isEqualTo(PolicyDecision.DENY);
  }

  @Test
  void prohibitedAlwaysDeniesRegardlessOfConfiguredRule() {
    assertThat(
            decide(RiskClassification.PROHIBITED, PolicyDecision.ALLOW, 0, Role.OWNER, false)
                .decision())
        .isEqualTo(PolicyDecision.DENY);
  }

  private PolicyEngine.DecisionResult decide(
      RiskClassification risk,
      PolicyDecision configured,
      int approvals,
      Role actor,
      boolean demoMode) {
    PolicyRule rule =
        PolicyRule.create(
            org,
            policy,
            risk,
            configured,
            approvals,
            List.of(Role.OWNER, Role.ADMIN, Role.RESPONDER),
            risk == RiskClassification.HIGH_RISK,
            15);
    return PolicyEngine.evaluate(rule, actor, Role.RESPONDER, risk, demoMode);
  }
}
