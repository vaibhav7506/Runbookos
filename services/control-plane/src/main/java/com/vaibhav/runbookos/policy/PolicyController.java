package com.vaibhav.runbookos.policy;

import com.vaibhav.runbookos.exception.AccessDeniedDomainException;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.runbook.RiskClassification;
import com.vaibhav.runbookos.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {
  private final PolicyEngine engine;

  public PolicyController(PolicyEngine engine) {
    this.engine = engine;
  }

  @GetMapping
  public List<PolicyEngine.PolicyView> list(@AuthenticationPrincipal AuthenticatedUser user) {
    return engine.list(requireOrg(user), user.userId());
  }

  @PutMapping("/{policyId}/rules/{risk}")
  public PolicyEngine.PolicyView update(
      @PathVariable UUID policyId,
      @PathVariable RiskClassification risk,
      @Valid @RequestBody RuleRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return engine.updateRule(
        requireOrg(user),
        policyId,
        risk,
        new PolicyEngine.UpdateRule(
            request.decision(),
            request.requiredApprovals(),
            request.approverRoles(),
            request.typedConfirmationRequired(),
            request.expirationMinutes()),
        user.userId());
  }

  private static UUID requireOrg(AuthenticatedUser user) {
    if (!user.hasOrganization())
      throw new AccessDeniedDomainException("Select an organization first");
    return user.organizationId();
  }

  public record RuleRequest(
      @NotNull PolicyDecision decision,
      @Min(0) @Max(5) int requiredApprovals,
      @NotEmpty List<Role> approverRoles,
      boolean typedConfirmationRequired,
      @Min(1) @Max(1440) int expirationMinutes) {}
}
