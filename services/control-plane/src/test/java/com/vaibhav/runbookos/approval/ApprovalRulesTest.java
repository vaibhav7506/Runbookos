package com.vaibhav.runbookos.approval;

import static org.assertj.core.api.Assertions.*;

import com.vaibhav.runbookos.exception.*;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.integration.IntegrationEnvironment;
import com.vaibhav.runbookos.policy.PolicyDecision;
import com.vaibhav.runbookos.runbook.RiskClassification;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApprovalRulesTest {
  @Test
  void elevatedActionRejectsSelfApprovalAndWrongPhrase() {
    UUID requester = UUID.randomUUID();
    ApprovalRequest request = highRisk(requester);
    assertThatThrownBy(
            () ->
                ApprovalService.validateDecisionEligibility(
                    request, Role.OWNER, requester, request.getConfirmationPhrase()))
        .isInstanceOf(AccessDeniedDomainException.class);
    assertThatThrownBy(
            () ->
                ApprovalService.validateDecisionEligibility(
                    request, Role.ADMIN, UUID.randomUUID(), "wrong"))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("phrase");
  }

  @Test
  void distinctEligibleUserWithExactPhraseCanApprove() {
    ApprovalRequest request = highRisk(UUID.randomUUID());
    assertThatCode(
            () ->
                ApprovalService.validateDecisionEligibility(
                    request, Role.ADMIN, UUID.randomUUID(), request.getConfirmationPhrase()))
        .doesNotThrowAnyException();
  }

  @Test
  void pendingApprovalCanExpireDeterministically() {
    ApprovalRequest request = highRisk(UUID.randomUUID());
    Instant expiredAt = request.getExpiresAt().plusSeconds(1);
    request.expire(expiredAt);
    assertThat(request.getStatus()).isEqualTo(ApprovalStatus.EXPIRED);
    assertThat(request.getResolvedAt()).isEqualTo(expiredAt);
  }

  private static ApprovalRequest highRisk(UUID requester) {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    return ApprovalRequest.pending(
        UUID.randomUUID(),
        UUID.randomUUID(),
        null,
        UUID.randomUUID(),
        "rollback",
        "Rollback checkout",
        IntegrationEnvironment.PRODUCTION,
        RiskClassification.HIGH_RISK,
        PolicyDecision.REQUIRE_ELEVATED_APPROVAL,
        "Two approvals required",
        2,
        "APPROVE Rollback checkout",
        requester,
        now,
        now.plusSeconds(900));
  }
}
