package com.vaibhav.runbookos.workflow;

import com.vaibhav.runbookos.audit.*;
import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.*;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.organization.TenantAccessService;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeadLetterService {
  private final OutboxEventRepository events;
  private final TenantAccessService access;
  private final AuditService audit;
  private final TimeProvider time;

  public DeadLetterService(
      OutboxEventRepository events,
      TenantAccessService access,
      AuditService audit,
      TimeProvider time) {
    this.events = events;
    this.access = access;
    this.audit = audit;
    this.time = time;
  }

  @Transactional(readOnly = true)
  public List<DeadLetterView> list(UUID org, UUID actor, int size) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    return events
        .findByOrganizationIdAndStatusOrderByCreatedAtDesc(
            org, OutboxStatus.DEAD, PageRequest.of(0, Math.min(Math.max(size, 1), 100)))
        .stream()
        .map(DeadLetterView::from)
        .toList();
  }

  @Transactional
  public DeadLetterView redrive(UUID org, UUID id, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    OutboxEvent event =
        events
            .findById(id)
            .filter(value -> value.getOrganizationId().equals(org))
            .orElseThrow(() -> new ResourceNotFoundException("dead-letter event", id));
    if (event.getStatus() != OutboxStatus.DEAD) {
      throw new ConflictException(
          "EVENT_NOT_DEAD", "Only dead-letter workflow events can be redriven");
    }
    event.redrive(time.nowTruncated());
    audit.recordSuccess("WORKFLOW_EVENT_REDRIVEN", "outbox_event", org, actor, id);
    return DeadLetterView.from(event);
  }

  public record DeadLetterView(
      UUID id,
      String aggregateType,
      UUID aggregateId,
      String eventType,
      int attempts,
      String lastError,
      OutboxStatus status,
      Instant createdAt) {
    static DeadLetterView from(OutboxEvent event) {
      return new DeadLetterView(
          event.getId(),
          event.getAggregateType(),
          event.getAggregateId(),
          event.getEventType(),
          event.getAttempts(),
          event.getLastError(),
          event.getStatus(),
          event.getCreatedAt());
    }
  }
}
