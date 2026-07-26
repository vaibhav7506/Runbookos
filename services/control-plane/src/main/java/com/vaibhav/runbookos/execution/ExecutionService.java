package com.vaibhav.runbookos.execution;

import com.vaibhav.runbookos.audit.*;
import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.*;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.incident.IncidentService;
import com.vaibhav.runbookos.organization.TenantAccessService;
import com.vaibhav.runbookos.workflow.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionService {
  private final ExecutionRepository executions;
  private final StepExecutionRepository steps;
  private final ExecutionEventRepository events;
  private final OutboxEventRepository outbox;
  private final WorkflowRegistry registry;
  private final IncidentService incidents;
  private final TenantAccessService access;
  private final AuditService audit;
  private final TimeProvider time;
  private final ExecutionEventStream stream;

  public ExecutionService(
      ExecutionRepository executions,
      StepExecutionRepository steps,
      ExecutionEventRepository events,
      OutboxEventRepository outbox,
      WorkflowRegistry registry,
      IncidentService incidents,
      TenantAccessService access,
      AuditService audit,
      TimeProvider time,
      ExecutionEventStream stream) {
    this.executions = executions;
    this.steps = steps;
    this.events = events;
    this.outbox = outbox;
    this.registry = registry;
    this.incidents = incidents;
    this.access = access;
    this.audit = audit;
    this.time = time;
    this.stream = stream;
  }

  @Transactional
  public Execution create(
      UUID org, UUID incidentId, String workflowKey, String idempotencyKey, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN, Role.RESPONDER);
    incidents.require(org, incidentId);
    if (idempotencyKey == null || idempotencyKey.isBlank())
      throw new ConflictException("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required");
    var existing = executions.findByOrganizationIdAndIdempotencyKey(org, idempotencyKey);
    if (existing.isPresent()) return existing.get();
    var definition = registry.require(workflowKey);
    Instant now = time.nowTruncated();
    Execution execution =
        executions.save(
            Execution.create(
                org,
                incidentId,
                workflowKey,
                definition.version(),
                idempotencyKey,
                definition.actionIds(),
                UUID.randomUUID().toString(),
                actor,
                now,
                now.plus(Duration.ofMinutes(15))));
    int sequence = 1;
    for (var step : definition.steps())
      steps.save(
          StepExecution.queued(
              org, execution.getId(), step.actionId(), step.name(), sequence++, 1));
    outbox.save(
        OutboxEvent.create(
            org,
            "execution",
            execution.getId(),
            "WORKFLOW_DISPATCH",
            Map.of("executionId", execution.getId().toString()),
            now));
    recordEvent(
        execution,
        null,
        "EXECUTION_CREATED",
        execution.getStatus().name(),
        "Execution queued",
        Map.of("workflow", workflowKey, "version", definition.version()));
    audit.recordSuccess(AuditActions.EXECUTION_CREATED, "execution", org, actor, execution.getId());
    return execution;
  }

  @Transactional(readOnly = true)
  public Execution require(UUID org, UUID id) {
    return executions
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("execution", id));
  }

  @Transactional(readOnly = true)
  public ExecutionTimeline timeline(UUID org, UUID id, UUID actor) {
    access.require(org, actor);
    Execution e = require(org, id);
    return new ExecutionTimeline(
        ExecutionView.from(e),
        steps.findByExecutionIdOrderBySequenceNumberAscAttemptAsc(id).stream()
            .map(StepView::from)
            .toList(),
        events.findByExecutionIdOrderByOccurredAtAscIdAsc(id).stream()
            .map(EventView::from)
            .toList());
  }

  @Transactional
  public Execution retry(UUID org, UUID id, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN, Role.RESPONDER);
    Execution e = require(org, id);
    e.transition(ExecutionStatus.QUEUED, time.nowTruncated());
    outbox.save(
        OutboxEvent.create(
            org,
            "execution",
            id,
            "WORKFLOW_DISPATCH",
            Map.of("executionId", id.toString(), "retry", true),
            time.nowTruncated()));
    recordEvent(
        e, null, "EXECUTION_RETRIED", e.getStatus().name(), "Execution manually retried", Map.of());
    audit.recordSuccess(AuditActions.EXECUTION_RETRIED, "execution", org, actor, id);
    return e;
  }

  @Transactional
  public StepExecution retryStep(UUID org, UUID id, String actionId, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN, Role.RESPONDER);
    Execution e = require(org, id);
    if (!e.getAllowedActionIds().contains(actionId))
      throw new AccessDeniedDomainException("Action is not authorized for this execution");
    StepExecution previous =
        steps
            .findFirstByExecutionIdAndActionIdOrderByAttemptDesc(id, actionId)
            .orElseThrow(() -> new ResourceNotFoundException("step", actionId));
    if (previous.getStatus() != StepStatus.FAILED)
      throw new ConflictException("STEP_NOT_RETRYABLE", "Only failed steps can be retried");
    StepExecution retry =
        steps.save(
            StepExecution.queued(
                org,
                id,
                actionId,
                previous.getName(),
                previous.getSequenceNumber(),
                previous.getAttempt() + 1));
    outbox.save(
        OutboxEvent.create(
            org,
            "execution",
            id,
            "STEP_RETRY",
            Map.of("executionId", id.toString(), "actionId", actionId),
            time.nowTruncated()));
    recordEvent(
        e,
        retry,
        "STEP_RETRY_QUEUED",
        retry.getStatus().name(),
        "Failed step queued for retry",
        Map.of("attempt", retry.getAttempt()));
    return retry;
  }

  @Transactional
  public void timeoutExpired() {
    Instant now = time.nowTruncated();
    for (Execution e :
        executions.findByStatusInAndTimeoutAtBefore(
            List.of(
                ExecutionStatus.QUEUED,
                ExecutionStatus.RUNNING,
                ExecutionStatus.PAUSED_FOR_APPROVAL),
            now)) {
      e.transition(ExecutionStatus.TIMED_OUT, now);
      recordEvent(
          e,
          null,
          "EXECUTION_TIMED_OUT",
          e.getStatus().name(),
          "Execution exceeded its deadline",
          Map.of());
    }
  }

  public ExecutionEvent recordEvent(
      Execution e,
      StepExecution step,
      String type,
      String status,
      String message,
      Map<String, Object> details) {
    ExecutionEvent event =
        events.save(
            ExecutionEvent.create(
                e.getOrganizationId(),
                e.getId(),
                step == null ? null : step.getId(),
                type,
                status,
                message,
                details,
                time.nowTruncated()));
    stream.publish(e.getId(), EventView.from(event));
    return event;
  }

  public record ExecutionView(
      UUID id,
      UUID incidentId,
      String workflowKey,
      int workflowVersion,
      ExecutionStatus status,
      List<String> allowedActionIds,
      Instant timeoutAt,
      Instant createdAt) {
    static ExecutionView from(Execution e) {
      return new ExecutionView(
          e.getId(),
          e.getIncidentId(),
          e.getWorkflowKey(),
          e.getWorkflowVersion(),
          e.getStatus(),
          e.getAllowedActionIds(),
          e.getTimeoutAt(),
          e.getCreatedAt());
    }
  }

  public record StepView(
      UUID id,
      String actionId,
      String name,
      int sequence,
      StepStatus status,
      int attempt,
      Instant startedAt,
      Instant completedAt,
      String errorCode,
      String errorMessage,
      Map<String, Object> output) {
    static StepView from(StepExecution s) {
      return new StepView(
          s.getId(),
          s.getActionId(),
          s.getName(),
          s.getSequenceNumber(),
          s.getStatus(),
          s.getAttempt(),
          s.getStartedAt(),
          s.getCompletedAt(),
          s.getErrorCode(),
          s.getErrorMessage(),
          s.getOutput());
    }
  }

  public record EventView(
      UUID id,
      UUID stepExecutionId,
      String type,
      String status,
      String message,
      Map<String, Object> details,
      Instant occurredAt) {
    static EventView from(ExecutionEvent e) {
      return new EventView(
          e.getId(),
          e.getStepExecutionId(),
          e.getEventType(),
          e.getStatus(),
          e.getMessage(),
          e.getDetails(),
          e.getOccurredAt());
    }
  }

  public record ExecutionTimeline(
      ExecutionView execution, List<StepView> steps, List<EventView> events) {}
}
