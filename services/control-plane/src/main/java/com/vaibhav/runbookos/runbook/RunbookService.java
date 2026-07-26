package com.vaibhav.runbookos.runbook;

import com.vaibhav.runbookos.audit.*;
import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.*;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.integration.IntegrationEnvironment;
import com.vaibhav.runbookos.organization.TenantAccessService;
import com.vaibhav.runbookos.policy.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RunbookService {
  private final RunbookRepository runbooks;
  private final RunbookVersionRepository versions;
  private final RunbookStepRepository steps;
  private final TenantAccessService access;
  private final PolicyEngine policyEngine;
  private final AuditService audit;
  private final TimeProvider time;

  public RunbookService(
      RunbookRepository runbooks,
      RunbookVersionRepository versions,
      RunbookStepRepository steps,
      TenantAccessService access,
      PolicyEngine policyEngine,
      AuditService audit,
      TimeProvider time) {
    this.runbooks = runbooks;
    this.versions = versions;
    this.steps = steps;
    this.access = access;
    this.policyEngine = policyEngine;
    this.audit = audit;
    this.time = time;
  }

  @Transactional
  public RunbookDetail create(UUID org, CreateRunbook request, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    Runbook runbook =
        runbooks.save(
            Runbook.create(
                org, request.name().trim(), request.description(), actor, time.nowTruncated()));
    RunbookVersion version =
        versions.save(
            RunbookVersion.draft(
                org, runbook.getId(), 1, "Initial draft", actor, time.nowTruncated()));
    audit.recordSuccess(AuditActions.RUNBOOK_CREATED, "runbook", org, actor, runbook.getId());
    return detail(org, runbook.getId(), actor, version.getId());
  }

  @Transactional(readOnly = true)
  public List<RunbookSummary> list(UUID org, UUID actor) {
    access.require(org, actor);
    return runbooks.findByOrganizationIdOrderByUpdatedAtDesc(org).stream()
        .map(
            runbook -> {
              var latest = versions.findByRunbookIdOrderByVersionNumberDesc(runbook.getId());
              return RunbookSummary.from(runbook, latest.isEmpty() ? null : latest.get(0));
            })
        .toList();
  }

  @Transactional(readOnly = true)
  public RunbookDetail detail(UUID org, UUID id, UUID actor, UUID requestedVersion) {
    access.require(org, actor);
    Runbook runbook = require(org, id);
    List<RunbookVersion> all = versions.findByRunbookIdOrderByVersionNumberDesc(id);
    RunbookVersion selected =
        requestedVersion == null
            ? all.stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("runbook version", id))
            : all.stream()
                .filter(value -> value.getId().equals(requestedVersion))
                .findFirst()
                .orElseThrow(
                    () -> new ResourceNotFoundException("runbook version", requestedVersion));
    return RunbookDetail.from(
        runbook,
        selected,
        steps.findByRunbookVersionIdOrderBySequenceNumberAsc(selected.getId()),
        all);
  }

  @Transactional
  public RunbookDetail replaceDraftSteps(
      UUID org, UUID runbookId, UUID versionId, List<RunbookStep.StepDraft> drafts, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    Runbook runbook = require(org, runbookId);
    RunbookVersion version = requireVersion(runbookId, versionId);
    version.requireDraft();
    validateSteps(drafts);
    steps.deleteByRunbookVersionId(versionId);
    drafts.stream()
        .sorted(Comparator.comparingInt(RunbookStep.StepDraft::sequenceNumber))
        .map(step -> RunbookStep.create(org, versionId, step))
        .forEach(steps::save);
    runbook.touch(time.nowTruncated());
    audit.recordSuccess(
        AuditActions.RUNBOOK_DRAFT_UPDATED, "runbook_version", org, actor, versionId);
    return detail(org, runbookId, actor, versionId);
  }

  @Transactional
  public RunbookDetail newDraft(UUID org, UUID runbookId, String summary, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    require(org, runbookId);
    List<RunbookVersion> all = versions.findByRunbookIdOrderByVersionNumberDesc(runbookId);
    if (all.stream().anyMatch(value -> value.getStatus() == RunbookVersionStatus.DRAFT))
      throw new ConflictException("DRAFT_EXISTS", "This runbook already has an editable draft");
    int number = all.stream().mapToInt(RunbookVersion::getVersionNumber).max().orElse(0) + 1;
    RunbookVersion draft =
        versions.save(
            RunbookVersion.draft(org, runbookId, number, summary, actor, time.nowTruncated()));
    all.stream()
        .filter(value -> value.getStatus() == RunbookVersionStatus.PUBLISHED)
        .findFirst()
        .ifPresent(
            published ->
                steps.findByRunbookVersionIdOrderBySequenceNumberAsc(published.getId()).stream()
                    .map(
                        old ->
                            RunbookStep.create(
                                org,
                                draft.getId(),
                                new RunbookStep.StepDraft(
                                    old.getStepKey(),
                                    old.getName(),
                                    old.getDescription(),
                                    old.getSequenceNumber(),
                                    old.getStepType(),
                                    old.getRiskClassification(),
                                    old.getRequiredRole(),
                                    old.getTimeoutSeconds(),
                                    old.getMaxRetries(),
                                    old.getRollbackInformation(),
                                    old.getAllowedEnvironments().stream()
                                        .map(IntegrationEnvironment::valueOf)
                                        .toList(),
                                    old.getConfiguration())))
                    .forEach(steps::save));
    return detail(org, runbookId, actor, draft.getId());
  }

  @Transactional
  public List<PolicyPreview> preview(
      UUID org, UUID runbookId, UUID versionId, IntegrationEnvironment environment, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    require(org, runbookId);
    requireVersion(runbookId, versionId);
    return steps.findByRunbookVersionIdOrderBySequenceNumberAsc(versionId).stream()
        .map(
            step ->
                new PolicyPreview(
                    step.getStepKey(),
                    step.getName(),
                    step.getRiskClassification(),
                    policyEngine.decide(
                        org,
                        null,
                        versionId,
                        step.getStepKey(),
                        environment,
                        step.getRiskClassification(),
                        step.getRequiredRole(),
                        actor)))
        .toList();
  }

  @Transactional
  public RunbookDetail publish(
      UUID org, UUID runbookId, UUID versionId, IntegrationEnvironment environment, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    Runbook runbook = require(org, runbookId);
    RunbookVersion version = requireVersion(runbookId, versionId);
    version.requireDraft();
    List<RunbookStep> draftSteps = steps.findByRunbookVersionIdOrderBySequenceNumberAsc(versionId);
    if (draftSteps.isEmpty())
      throw new ConflictException("EMPTY_RUNBOOK", "A runbook needs at least one step");
    List<PolicyPreview> preview = preview(org, runbookId, versionId, environment, actor);
    if (preview.stream().anyMatch(item -> item.policy().decision() == PolicyDecision.DENY))
      throw new ConflictException(
          "POLICY_PREVIEW_DENIED", "Policy preview contains a denied action");
    versions
        .findFirstByRunbookIdAndStatusOrderByVersionNumberDesc(
            runbookId, RunbookVersionStatus.PUBLISHED)
        .ifPresent(RunbookVersion::supersede);
    version.publish(actor, time.nowTruncated());
    runbook.touch(time.nowTruncated());
    audit.recordSuccess(AuditActions.RUNBOOK_PUBLISHED, "runbook_version", org, actor, versionId);
    return detail(org, runbookId, actor, versionId);
  }

  private Runbook require(UUID org, UUID id) {
    return runbooks
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("runbook", id));
  }

  private RunbookVersion requireVersion(UUID runbookId, UUID versionId) {
    return versions
        .findById(versionId)
        .filter(value -> value.getRunbookId().equals(runbookId))
        .orElseThrow(() -> new ResourceNotFoundException("runbook version", versionId));
  }

  private static void validateSteps(List<RunbookStep.StepDraft> drafts) {
    if (drafts.isEmpty()) throw new ConflictException("EMPTY_RUNBOOK", "Add at least one step");
    Set<String> keys = new HashSet<>();
    Set<Integer> sequence = new HashSet<>();
    for (RunbookStep.StepDraft step : drafts) {
      if (!keys.add(step.stepKey()) || !sequence.add(step.sequenceNumber()))
        throw new ConflictException(
            "DUPLICATE_RUNBOOK_STEP", "Step keys and sequence numbers must be unique");
      if (step.riskClassification() != RiskClassification.READ_ONLY
          && (step.rollbackInformation() == null || step.rollbackInformation().isBlank()))
        throw new ConflictException(
            "ROLLBACK_INFORMATION_REQUIRED",
            "Non-read-only actions must document rollback information");
      if (step.allowedEnvironments().isEmpty())
        throw new ConflictException(
            "ENVIRONMENT_REQUIRED", "Every step must declare an allowed environment");
    }
  }

  public record CreateRunbook(String name, String description) {}

  public record PolicyPreview(
      String stepKey, String name, RiskClassification risk, PolicyEngine.DecisionResult policy) {}

  public record RunbookSummary(
      UUID id,
      String name,
      String description,
      Integer latestVersion,
      RunbookVersionStatus latestStatus,
      java.time.Instant updatedAt) {
    static RunbookSummary from(Runbook runbook, RunbookVersion latest) {
      return new RunbookSummary(
          runbook.getId(),
          runbook.getName(),
          runbook.getDescription(),
          latest == null ? null : latest.getVersionNumber(),
          latest == null ? null : latest.getStatus(),
          runbook.getUpdatedAt());
    }
  }

  public record VersionView(
      UUID id,
      int versionNumber,
      RunbookVersionStatus status,
      String changeSummary,
      java.time.Instant createdAt,
      java.time.Instant publishedAt) {
    static VersionView from(RunbookVersion value) {
      return new VersionView(
          value.getId(),
          value.getVersionNumber(),
          value.getStatus(),
          value.getChangeSummary(),
          value.getCreatedAt(),
          value.getPublishedAt());
    }
  }

  public record RunbookStepView(
      UUID id,
      String stepKey,
      String name,
      String description,
      int sequenceNumber,
      RunbookStepType stepType,
      RiskClassification riskClassification,
      Role requiredRole,
      int timeoutSeconds,
      int maxRetries,
      String rollbackInformation,
      List<String> allowedEnvironments,
      Map<String, Object> configuration) {
    static RunbookStepView from(RunbookStep value) {
      return new RunbookStepView(
          value.getId(),
          value.getStepKey(),
          value.getName(),
          value.getDescription(),
          value.getSequenceNumber(),
          value.getStepType(),
          value.getRiskClassification(),
          value.getRequiredRole(),
          value.getTimeoutSeconds(),
          value.getMaxRetries(),
          value.getRollbackInformation(),
          value.getAllowedEnvironments(),
          value.getConfiguration());
    }
  }

  public record RunbookDetail(
      RunbookSummary runbook,
      VersionView selectedVersion,
      List<RunbookStepView> steps,
      List<VersionView> versions) {
    static RunbookDetail from(
        Runbook runbook,
        RunbookVersion selected,
        List<RunbookStep> steps,
        List<RunbookVersion> versions) {
      return new RunbookDetail(
          RunbookSummary.from(runbook, versions.isEmpty() ? null : versions.get(0)),
          VersionView.from(selected),
          steps.stream().map(RunbookStepView::from).toList(),
          versions.stream().map(VersionView::from).toList());
    }
  }
}
