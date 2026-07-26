package com.vaibhav.runbookos.approval;

import com.vaibhav.runbookos.exception.AccessDeniedDomainException;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.integration.IntegrationEnvironment;
import com.vaibhav.runbookos.runbook.RiskClassification;
import com.vaibhav.runbookos.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {
  private final ApprovalService service;

  public ApprovalController(ApprovalService service) {
    this.service = service;
  }

  @PostMapping("/authorize")
  @ResponseStatus(HttpStatus.CREATED)
  public ApprovalService.AuthorizationResult authorize(
      @Valid @RequestBody AuthorizationRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return service.authorize(
        requireOrg(user),
        new ApprovalService.AuthorizeAction(
            request.incidentId(),
            request.executionId(),
            request.runbookVersionId(),
            request.stepKey(),
            request.actionName(),
            request.environment(),
            request.riskClassification(),
            request.requiredRole()),
        user.userId());
  }

  @GetMapping
  public List<ApprovalService.ApprovalView> inbox(
      @RequestParam(required = false) ApprovalStatus status,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return service.inbox(requireOrg(user), status, user.userId());
  }

  @GetMapping("/{id}")
  public ApprovalService.ApprovalView detail(
      @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
    return service.detail(requireOrg(user), id, user.userId());
  }

  @PostMapping("/{id}/decisions")
  public ApprovalService.ApprovalView decide(
      @PathVariable UUID id,
      @Valid @RequestBody DecisionRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return service.decide(
        requireOrg(user),
        id,
        new ApprovalService.DecisionInput(
            request.decision(), request.reason(), request.confirmationPhrase()),
        user.userId());
  }

  private static UUID requireOrg(AuthenticatedUser user) {
    if (!user.hasOrganization())
      throw new AccessDeniedDomainException("Select an organization first");
    return user.organizationId();
  }

  public record AuthorizationRequest(
      UUID incidentId,
      UUID executionId,
      UUID runbookVersionId,
      @NotBlank String stepKey,
      @NotBlank String actionName,
      @NotNull IntegrationEnvironment environment,
      @NotNull RiskClassification riskClassification,
      @NotNull Role requiredRole) {}

  public record DecisionRequest(
      @NotNull ApprovalChoice decision,
      @Size(max = 1000) String reason,
      String confirmationPhrase) {}
}
