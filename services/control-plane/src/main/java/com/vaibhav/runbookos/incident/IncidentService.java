package com.vaibhav.runbookos.incident;

import com.vaibhav.runbookos.approval.ApprovalService;
import com.vaibhav.runbookos.audit.*;
import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.ResourceNotFoundException;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.organization.TenantAccessService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {
  private final IncidentRepository incidents;
  private final IncidentSignalRepository signals;
  private final IncidentEvidenceRepository evidence;
  private final IncidentCommentRepository comments;
  private final IncidentAssignmentRepository assignments;
  private final TenantAccessService access;
  private final AuditService audit;
  private final TimeProvider time;
  private final ApprovalService approvals;

  public IncidentService(
      IncidentRepository incidents,
      IncidentSignalRepository signals,
      IncidentEvidenceRepository evidence,
      IncidentCommentRepository comments,
      IncidentAssignmentRepository assignments,
      TenantAccessService access,
      AuditService audit,
      TimeProvider time,
      ApprovalService approvals) {
    this.incidents = incidents;
    this.signals = signals;
    this.evidence = evidence;
    this.comments = comments;
    this.assignments = assignments;
    this.access = access;
    this.audit = audit;
    this.time = time;
    this.approvals = approvals;
  }

  @Transactional
  public Incident ingest(
      UUID org, SignalSource source, String externalId, Map<String, Object> payload) {
    if (externalId != null) {
      var existing = signals.findByOrganizationIdAndSourceAndExternalId(org, source, externalId);
      if (existing.isPresent()) return require(org, existing.get().getIncidentId());
    }
    Instant now = time.nowTruncated();
    String title = text(payload, "title", text(payload, "message", "Incident signal"));
    String service = text(payload, "service", text(payload, "repository", "unknown-service"));
    String fingerprint =
        fingerprint(
            service
                + "|"
                + text(payload, "errorType", text(payload, "event", "unknown"))
                + "|"
                + title.replaceAll("\\d+", "#").toLowerCase(Locale.ROOT));
    Incident incident =
        incidents
            .findFirstByOrganizationIdAndFingerprintAndStatusNotInAndLastSignalAtAfterOrderByLastSignalAtDesc(
                org,
                fingerprint,
                List.of(IncidentStatus.CLOSED, IncidentStatus.CANCELLED),
                now.minus(Duration.ofMinutes(30)))
            .orElse(null);
    boolean grouped = incident != null;
    if (grouped) incident.recordSignal(now);
    else
      incident =
          incidents.save(
              Incident.create(
                  org,
                  title,
                  text(payload, "summary", null),
                  severity(payload),
                  priority(payload),
                  service,
                  fingerprint,
                  now));
    signals.save(
        IncidentSignal.create(
            org, incident.getId(), source, externalId, fingerprint, payload, now));
    audit.record(
        AuditRecord.builder(
                grouped ? AuditActions.INCIDENT_GROUPED : AuditActions.INCIDENT_CREATED, "incident")
            .organization(org)
            .actorType(ActorType.INTEGRATION)
            .actorLabel(source.name())
            .resourceId(incident.getId())
            .metadata("fingerprint", fingerprint)
            .build());
    return incident;
  }

  @Transactional(readOnly = true)
  public Incident require(UUID org, UUID id) {
    return incidents
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("incident", id));
  }

  @Transactional(readOnly = true)
  public IncidentPage search(
      UUID org,
      UUID user,
      IncidentStatus status,
      IncidentSeverity severity,
      String service,
      String query,
      Instant cursor,
      int size) {
    access.require(org, user);
    var slice =
        incidents.search(
            org,
            status,
            severity,
            blankToNull(service),
            blankToNull(query),
            cursor,
            PageRequest.of(0, Math.min(Math.max(size, 1), 100)));
    List<IncidentView> items = slice.getContent().stream().map(IncidentView::from).toList();
    String next =
        slice.hasNext() && !items.isEmpty()
            ? items.get(items.size() - 1).lastSignalAt().toString()
            : null;
    return new IncidentPage(items, next, slice.hasNext());
  }

  @Transactional
  public Incident transition(UUID org, UUID id, IncidentStatus target, UUID user) {
    access.require(org, user, Role.OWNER, Role.ADMIN, Role.RESPONDER);
    Incident value = require(org, id);
    IncidentStatus from = value.getStatus();
    value.transition(target, time.nowTruncated());
    approvals.cancelForIncident(org, id, "Incident state changed from " + from + " to " + target);
    audit.record(
        AuditRecord.builder(AuditActions.INCIDENT_TRANSITIONED, "incident")
            .organization(org)
            .actor(user, null)
            .resourceId(id)
            .metadata("from", from)
            .metadata("to", target)
            .build());
    return value;
  }

  @Transactional
  public Incident assign(UUID org, UUID id, UUID assignee, UUID user) {
    access.require(org, user, Role.OWNER, Role.ADMIN, Role.RESPONDER);
    access.require(org, assignee);
    Incident value = require(org, id);
    Instant now = time.nowTruncated();
    value.assign(assignee, now);
    assignments.save(IncidentAssignment.create(org, id, assignee, user, now));
    audit.recordSuccess(AuditActions.INCIDENT_ASSIGNED, "incident", org, user, id);
    return value;
  }

  @Transactional
  public IncidentComment addComment(UUID org, UUID id, String body, UUID user) {
    access.require(org, user, Role.OWNER, Role.ADMIN, Role.RESPONDER);
    require(org, id);
    IncidentComment comment =
        comments.save(IncidentComment.create(org, id, user, body.trim(), time.nowTruncated()));
    audit.recordSuccess(AuditActions.INCIDENT_COMMENTED, "incident", org, user, id);
    return comment;
  }

  @Transactional
  public void addDemoEvidence(UUID org, UUID incidentId, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN, Role.RESPONDER);
    require(org, incidentId);
    Instant now = time.nowTruncated();
    evidence.save(
        IncidentEvidence.create(
            org,
            incidentId,
            "DEPLOYMENT",
            "checkout-api deployment 2026.07.26.3",
            Map.of(
                "deployedAt",
                now.minusSeconds(420).toString(),
                "commit",
                "7f4a8d2",
                "errorRateBefore",
                0.3,
                "errorRateAfter",
                8.7,
                "authorization",
                "Bearer demo-value-that-must-never-reach-a-model"),
            "DEMO",
            now));
    evidence.save(
        IncidentEvidence.create(
            org,
            incidentId,
            "SERVICE_METRIC",
            "Checkout 5xx rate",
            Map.of("window", "10m", "baselinePercent", 0.3, "currentPercent", 8.7),
            "DEMO",
            now.plusMillis(1)));
  }

  @Transactional(readOnly = true)
  public IncidentDetail detail(UUID org, UUID id, UUID user) {
    access.require(org, user);
    Incident value = require(org, id);
    return new IncidentDetail(
        IncidentView.from(value),
        evidence.findByIncidentIdOrderByCollectedAtAsc(id).stream()
            .map(EvidenceView::from)
            .toList(),
        comments.findByIncidentIdOrderByCreatedAtAsc(id).stream().map(CommentView::from).toList());
  }

  private static String text(Map<String, Object> p, String key, String fallback) {
    Object v = p.get(key);
    return v instanceof String s && !s.isBlank() ? s : fallback;
  }

  private static IncidentSeverity severity(Map<String, Object> p) {
    try {
      return IncidentSeverity.valueOf(text(p, "severity", "SEV3").toUpperCase(Locale.ROOT));
    } catch (Exception e) {
      return IncidentSeverity.SEV3;
    }
  }

  private static int priority(Map<String, Object> p) {
    Object v = p.get("priority");
    return v instanceof Number n ? Math.min(4, Math.max(1, n.intValue())) : 3;
  }

  private static String fingerprint(String value) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  public record IncidentView(
      UUID id,
      String title,
      String summary,
      IncidentStatus status,
      IncidentSeverity severity,
      int priority,
      String affectedService,
      UUID assigneeUserId,
      int signalCount,
      Instant firstDetectedAt,
      Instant lastSignalAt) {
    static IncidentView from(Incident i) {
      return new IncidentView(
          i.getId(),
          i.getTitle(),
          i.getSummary(),
          i.getStatus(),
          i.getSeverity(),
          i.getPriority(),
          i.getAffectedService(),
          i.getAssigneeUserId(),
          i.getSignalCount(),
          i.getFirstDetectedAt(),
          i.getLastSignalAt());
    }
  }

  public record IncidentPage(List<IncidentView> items, String nextCursor, boolean hasMore) {}

  public record EvidenceView(
      UUID id, String kind, String title, Map<String, Object> content, Instant collectedAt) {
    static EvidenceView from(IncidentEvidence e) {
      return new EvidenceView(
          e.getId(), e.getKind(), e.getTitle(), e.getContent(), e.getCollectedAt());
    }
  }

  public record CommentView(UUID id, UUID authorUserId, String body, Instant createdAt) {
    static CommentView from(IncidentComment c) {
      return new CommentView(c.getId(), c.getAuthorUserId(), c.getBody(), c.getCreatedAt());
    }
  }

  public record IncidentDetail(
      IncidentView incident, List<EvidenceView> evidence, List<CommentView> comments) {}
}
