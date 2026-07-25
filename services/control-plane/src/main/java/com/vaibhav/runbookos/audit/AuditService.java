package com.vaibhav.runbookos.audit;

import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.config.CorrelationIdFilter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes and reads the immutable audit trail.
 *
 * <p>Writes run in a {@link Propagation#REQUIRES_NEW} transaction. That matters for the denial and
 * failure paths: when a service rejects an operation and throws, the surrounding transaction rolls
 * back, and an audit row written in that same transaction would vanish with it. A separate
 * transaction means the record of the denial survives.
 */
@Service
public class AuditService {

  private static final Logger log = LoggerFactory.getLogger(AuditService.class);

  private final AuditEventRepository repository;
  private final TimeProvider timeProvider;

  public AuditService(AuditEventRepository repository, TimeProvider timeProvider) {
    this.repository = repository;
    this.timeProvider = timeProvider;
  }

  /**
   * Persists an audit event. The correlation ID is taken from the current request context so an
   * audit row can be tied back to application logs.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AuditEvent record(AuditRecord record) {
    AuditEvent event =
        new AuditEvent(
            record.organizationId(),
            record.actorUserId(),
            record.actorType(),
            record.actorLabel(),
            record.action(),
            record.resourceType(),
            record.resourceId(),
            record.outcome(),
            record.reason(),
            MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY),
            record.ipAddress(),
            record.metadata(),
            record.occurredAt() == null ? timeProvider.nowTruncated() : record.occurredAt());
    AuditEvent saved = repository.save(event);
    log.debug(
        "Recorded audit event action={} resourceType={} outcome={}",
        saved.getAction(),
        saved.getResourceType(),
        saved.getOutcome());
    return saved;
  }

  /**
   * Convenience for the common success case.
   *
   * @param resourceId may be null for actions with no single target
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AuditEvent recordSuccess(
      String action, String resourceType, UUID organizationId, UUID actorUserId, UUID resourceId) {
    return record(
        AuditRecord.builder(action, resourceType)
            .organization(organizationId)
            .actor(actorUserId, null)
            .resourceId(resourceId)
            .outcome(AuditOutcome.SUCCESS)
            .build());
  }

  /** Records an authorization denial. Denials are always audited, never only logged. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AuditEvent recordDenied(
      String action, String resourceType, UUID organizationId, UUID actorUserId, String reason) {
    return record(
        AuditRecord.builder(action, resourceType)
            .organization(organizationId)
            .actor(actorUserId, null)
            .outcome(AuditOutcome.DENIED)
            .reason(reason)
            .build());
  }

  /**
   * Tenant-scoped listing. The organization identifier must come from the authenticated tenant
   * context, never from a request parameter.
   */
  @Transactional(readOnly = true)
  public Page<AuditEvent> findForOrganization(UUID organizationId, Pageable pageable) {
    return repository.findByOrganizationIdOrderByOccurredAtDesc(organizationId, pageable);
  }

  @Transactional(readOnly = true)
  public Page<AuditEvent> findForOrganizationAndAction(
      UUID organizationId, String action, Pageable pageable) {
    return repository.findByOrganizationIdAndActionOrderByOccurredAtDesc(
        organizationId, action, pageable);
  }
}
