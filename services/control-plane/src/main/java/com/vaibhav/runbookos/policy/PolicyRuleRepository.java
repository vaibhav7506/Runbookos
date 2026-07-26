package com.vaibhav.runbookos.policy;

import com.vaibhav.runbookos.runbook.RiskClassification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRuleRepository extends JpaRepository<PolicyRule, UUID> {
  Optional<PolicyRule> findByPolicyIdAndRiskClassification(UUID policyId, RiskClassification risk);

  List<PolicyRule> findByPolicyIdOrderByRiskClassificationAsc(UUID policyId);
}
