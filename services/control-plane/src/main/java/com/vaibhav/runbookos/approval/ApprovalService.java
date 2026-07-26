package com.vaibhav.runbookos.approval;

import com.vaibhav.runbookos.audit.*;
import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.*;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.integration.IntegrationEnvironment;
import com.vaibhav.runbookos.organization.TenantAccessService;
import com.vaibhav.runbookos.policy.*;
import com.vaibhav.runbookos.runbook.RiskClassification;
import com.vaibhav.runbookos.workflow.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {
  private final ApprovalRequestRepository requests;
  private final ApprovalDecisionRepository decisions;
  private final PolicyEngine policy;
  private final TenantAccessService access;
  private final OutboxEventRepository outbox;
  private final AuditService audit;
  private final TimeProvider time;

  public ApprovalService(
      ApprovalRequestRepository requests,
      ApprovalDecisionRepository decisions,
      PolicyEngine policy,
      TenantAccessService access,
      OutboxEventRepository outbox,
      AuditService audit,
      TimeProvider time) {
    this.requests = requests;
    this.decisions = decisions;
    this.policy = policy;
    this.access = access;
    this.outbox = outbox;
    this.audit = audit;
    this.time = time;
  }

  @Transactional
  public AuthorizationResult authorize(UUID org, AuthorizeAction action, UUID actor) {
    PolicyEngine.DecisionResult decision =
        policy.decide(
            org,
            action.incidentId(),
            action.runbookVersionId(),
            action.stepKey(),
            action.environment(),
            action.riskClassification(),
            action.requiredRole(),
            actor);
    if (decision.decision() == PolicyDecision.ALLOW)
      return new AuthorizationResult(decision, null, true);
    if (decision.decision() == PolicyDecision.DENY)
      return new AuthorizationResult(decision, null, false);
    Instant now = time.nowTruncated();
    String phrase = decision.typedConfirmationRequired() ? "APPROVE " + action.actionName() : null;
    ApprovalRequest request =
        requests.save(
            ApprovalRequest.pending(
                org,
                action.incidentId(),
                action.executionId(),
                action.runbookVersionId(),
                action.stepKey(),
                action.actionName(),
                action.environment(),
                action.riskClassification(),
                decision.decision(),
                decision.reason(),
                decision.requiredApprovals(),
                phrase,
                actor,
                now,
                now.plusSeconds(decision.expirationMinutes() * 60L)));
    outbox.save(
        OutboxEvent.create(
            org,
            "approval",
            request.getId(),
            "APPROVAL_REQUESTED",
            Map.of(
                "approvalId", request.getId().toString(),
                "actionName", request.getActionName(),
                "risk", request.getRiskClassification().name(),
                "expiresAt", request.getExpiresAt().toString()),
            now));
    audit.recordSuccess(
        AuditActions.APPROVAL_REQUESTED, "approval_request", org, actor, request.getId());
    return new AuthorizationResult(decision, ApprovalView.from(request, List.of()), false);
  }

  @Transactional(readOnly = true)
  public List<ApprovalView> inbox(UUID org, ApprovalStatus status, UUID actor) {
    access.require(org, actor);
    List<ApprovalRequest> values =
        status == null
            ? requests.findByOrganizationIdOrderByRequestedAtDesc(org)
            : requests.findByOrganizationIdAndStatusOrderByRequestedAtDesc(org, status);
    return values.stream().map(this::view).toList();
  }

  @Transactional(readOnly = true)
  public ApprovalView detail(UUID org, UUID id, UUID actor) {
    access.require(org, actor);
    return view(require(org, id));
  }

  @Transactional
  public ApprovalView decide(UUID org, UUID id, DecisionInput input, UUID actor) {
    Role actorRole = access.require(org, actor).getRole();
    ApprovalRequest request = require(org, id);
    Instant now = time.nowTruncated();
    requirePending(request, now);
    validateDecisionEligibility(request, actorRole, actor, input.confirmationPhrase());
    if (decisions.findByApprovalRequestIdAndApproverUserId(id, actor).isPresent())
      throw new ConflictException("APPROVAL_ALREADY_DECIDED", "You already decided this request");
    decisions.save(
        ApprovalDecisionRecord.create(org, id, actor, input.decision(), input.reason(), now));
    if (input.decision() == ApprovalChoice.DENY) request.deny(now);
    else if (decisions.countByApprovalRequestIdAndDecision(id, ApprovalChoice.APPROVE)
        >= request.getRequiredApprovals()) request.approve(now);
    audit.record(
        AuditRecord.builder(AuditActions.APPROVAL_DECIDED, "approval_request")
            .organization(org)
            .actor(actor, null)
            .resourceId(id)
            .metadata("decision", input.decision())
            .metadata("status", request.getStatus())
            .build());
    return view(request);
  }

  @Transactional
  public int cancelForIncident(UUID org, UUID incidentId, String reason) {
    int count = 0;
    for (ApprovalRequest request :
        requests.findByOrganizationIdAndIncidentIdAndStatus(
            org, incidentId, ApprovalStatus.PENDING)) {
      request.cancel(reason, time.nowTruncated());
      audit.record(
          AuditRecord.builder(AuditActions.APPROVAL_CANCELLED, "approval_request")
              .organization(org)
              .actorType(ActorType.SYSTEM)
              .resourceId(request.getId())
              .reason(reason)
              .build());
      count++;
    }
    return count;
  }

  @Transactional
  public int expirePending() {
    int count = 0;
    Instant now = time.nowTruncated();
    for (ApprovalRequest request :
        requests.findByStatusAndExpiresAtBefore(ApprovalStatus.PENDING, now)) {
      request.expire(now);
      audit.record(
          AuditRecord.builder(AuditActions.APPROVAL_EXPIRED, "approval_request")
              .organization(request.getOrganizationId())
              .actorType(ActorType.SYSTEM)
              .resourceId(request.getId())
              .build());
      count++;
    }
    return count;
  }

  private ApprovalRequest require(UUID org, UUID id) {
    return requests
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("approval", id));
  }

  private static void requirePending(ApprovalRequest request, Instant now) {
    if (request.getStatus() != ApprovalStatus.PENDING)
      throw new ConflictException("APPROVAL_NOT_PENDING", "This approval is no longer pending");
    if (!request.getExpiresAt().isAfter(now)) {
      request.expire(now);
      throw new ConflictException("APPROVAL_EXPIRED", "This approval has expired");
    }
  }

  private static boolean eligible(Role role, RiskClassification risk) {
    return risk == RiskClassification.HIGH_RISK
        ? role == Role.OWNER || role == Role.ADMIN
        : role.canApprove();
  }

  static void validateDecisionEligibility(
      ApprovalRequest request, Role role, UUID actor, String confirmationPhrase) {
    if (!eligible(role, request.getRiskClassification()))
      throw new AccessDeniedDomainException("Your role cannot approve this action");
    if (request.getRiskClassification() == RiskClassification.HIGH_RISK
        && request.getRequestedBy().equals(actor))
      throw new AccessDeniedDomainException("Elevated actions cannot be self-approved");
    if (request.getConfirmationPhrase() != null
        && !request.getConfirmationPhrase().equals(confirmationPhrase))
      throw new ConflictException(
          "CONFIRMATION_PHRASE_MISMATCH", "The typed confirmation phrase does not match");
  }

  private ApprovalView view(ApprovalRequest request) {
    return ApprovalView.from(
        request, decisions.findByApprovalRequestIdOrderByDecidedAtAsc(request.getId()));
  }

  public record AuthorizeAction(
      UUID incidentId,
      UUID executionId,
      UUID runbookVersionId,
      String stepKey,
      String actionName,
      IntegrationEnvironment environment,
      RiskClassification riskClassification,
      Role requiredRole) {}

  public record DecisionInput(ApprovalChoice decision, String reason, String confirmationPhrase) {}

  public record AuthorizationResult(
      PolicyEngine.DecisionResult policy, ApprovalView approval, boolean executable) {}

  public record DecisionView(
      UUID approverUserId, ApprovalChoice decision, String reason, Instant decidedAt) {
    static DecisionView from(ApprovalDecisionRecord value) {
      return new DecisionView(
          value.getApproverUserId(), value.getDecision(), value.getReason(), value.getDecidedAt());
    }
  }

  public record ApprovalView(
      UUID id,
      UUID incidentId,
      UUID executionId,
      UUID runbookVersionId,
      String stepKey,
      String actionName,
      IntegrationEnvironment environment,
      RiskClassification riskClassification,
      PolicyDecision policyDecision,
      String policyReason,
      ApprovalStatus status,
      int requiredApprovals,
      String confirmationPhrase,
      UUID requestedBy,
      Instant requestedAt,
      Instant expiresAt,
      Instant resolvedAt,
      String cancellationReason,
      List<DecisionView> decisions) {
    static ApprovalView from(ApprovalRequest value, List<ApprovalDecisionRecord> decisions) {
      return new ApprovalView(
          value.getId(),
          value.getIncidentId(),
          value.getExecutionId(),
          value.getRunbookVersionId(),
          value.getStepKey(),
          value.getActionName(),
          value.getEnvironment(),
          value.getRiskClassification(),
          value.getPolicyDecision(),
          value.getPolicyReason(),
          value.getStatus(),
          value.getRequiredApprovals(),
          value.getConfirmationPhrase(),
          value.getRequestedBy(),
          value.getRequestedAt(),
          value.getExpiresAt(),
          value.getResolvedAt(),
          value.getCancellationReason(),
          decisions.stream().map(DecisionView::from).toList());
    }
  }
}
