package com.vaibhav.runbookos.runbook;

import com.vaibhav.runbookos.exception.AccessDeniedDomainException;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.integration.IntegrationEnvironment;
import com.vaibhav.runbookos.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/runbooks")
public class RunbookController {
  private final RunbookService service;

  public RunbookController(RunbookService service) {
    this.service = service;
  }

  @GetMapping
  public List<RunbookService.RunbookSummary> list(@AuthenticationPrincipal AuthenticatedUser user) {
    return service.list(requireOrg(user), user.userId());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public RunbookService.RunbookDetail create(
      @Valid @RequestBody CreateRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
    return service.create(
        requireOrg(user),
        new RunbookService.CreateRunbook(request.name(), request.description()),
        user.userId());
  }

  @GetMapping("/{id}")
  public RunbookService.RunbookDetail detail(
      @PathVariable UUID id,
      @RequestParam(required = false) UUID versionId,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return service.detail(requireOrg(user), id, user.userId(), versionId);
  }

  @PostMapping("/{id}/versions")
  @ResponseStatus(HttpStatus.CREATED)
  public RunbookService.RunbookDetail newDraft(
      @PathVariable UUID id,
      @RequestBody NewVersionRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return service.newDraft(requireOrg(user), id, request.changeSummary(), user.userId());
  }

  @PutMapping("/{id}/versions/{versionId}/steps")
  public RunbookService.RunbookDetail replaceSteps(
      @PathVariable UUID id,
      @PathVariable UUID versionId,
      @Valid @RequestBody StepsRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    List<RunbookStep.StepDraft> steps =
        request.steps().stream()
            .map(
                step ->
                    new RunbookStep.StepDraft(
                        step.stepKey(),
                        step.name(),
                        step.description(),
                        step.sequenceNumber(),
                        step.stepType(),
                        step.riskClassification(),
                        step.requiredRole(),
                        step.timeoutSeconds(),
                        step.maxRetries(),
                        step.rollbackInformation(),
                        step.allowedEnvironments(),
                        step.configuration()))
            .toList();
    return service.replaceDraftSteps(requireOrg(user), id, versionId, steps, user.userId());
  }

  @PostMapping("/{id}/versions/{versionId}/policy-preview")
  public List<RunbookService.PolicyPreview> preview(
      @PathVariable UUID id,
      @PathVariable UUID versionId,
      @RequestBody EnvironmentRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return service.preview(requireOrg(user), id, versionId, request.environment(), user.userId());
  }

  @PostMapping("/{id}/versions/{versionId}/publish")
  public RunbookService.RunbookDetail publish(
      @PathVariable UUID id,
      @PathVariable UUID versionId,
      @RequestBody EnvironmentRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return service.publish(requireOrg(user), id, versionId, request.environment(), user.userId());
  }

  private static UUID requireOrg(AuthenticatedUser user) {
    if (!user.hasOrganization())
      throw new AccessDeniedDomainException("Select an organization first");
    return user.organizationId();
  }

  public record CreateRequest(@NotBlank @Size(max = 160) String name, String description) {}

  public record NewVersionRequest(@Size(max = 512) String changeSummary) {}

  public record EnvironmentRequest(@NotNull IntegrationEnvironment environment) {}

  public record StepsRequest(@NotEmpty List<@Valid StepRequest> steps) {}

  public record StepRequest(
      @NotBlank @Size(max = 80) String stepKey,
      @NotBlank @Size(max = 160) String name,
      String description,
      @Min(1) int sequenceNumber,
      @NotNull RunbookStepType stepType,
      @NotNull RiskClassification riskClassification,
      @NotNull Role requiredRole,
      @Min(1) @Max(3600) int timeoutSeconds,
      @Min(0) @Max(5) int maxRetries,
      String rollbackInformation,
      @NotEmpty List<IntegrationEnvironment> allowedEnvironments,
      Map<String, Object> configuration) {}
}
