package com.vaibhav.runbookos.audit;

import com.vaibhav.runbookos.exception.AccessDeniedDomainException;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.organization.TenantAccessService;
import com.vaibhav.runbookos.security.AuthenticatedUser;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
  private final AuditEventRepository events;
  private final TenantAccessService access;

  public AuditController(AuditEventRepository events, TenantAccessService access) {
    this.events = events;
    this.access = access;
  }

  @GetMapping
  public AuditPage list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      @AuthenticationPrincipal AuthenticatedUser user) {
    UUID org = requireOrg(user);
    access.require(org, user.userId(), Role.OWNER, Role.ADMIN, Role.AUDITOR);
    var result =
        events.findByOrganizationIdOrderByOccurredAtDesc(
            org, PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100)));
    return new AuditPage(
        result.getContent().stream().map(AuditView::from).toList(),
        result.getNumber(),
        result.getTotalPages(),
        result.getTotalElements());
  }

  private static UUID requireOrg(AuthenticatedUser user) {
    if (!user.hasOrganization())
      throw new AccessDeniedDomainException("Select an organization first");
    return user.organizationId();
  }

  public record AuditPage(List<AuditView> items, int page, int totalPages, long totalItems) {}

  public record AuditView(
      UUID id,
      String action,
      String resourceType,
      String resourceId,
      AuditOutcome outcome,
      String actor,
      String correlationId,
      String integrityHash,
      Map<String, Object> metadata,
      Instant occurredAt) {
    static AuditView from(AuditEvent event) {
      return new AuditView(
          event.getId(),
          event.getAction(),
          event.getResourceType(),
          event.getResourceId(),
          event.getOutcome(),
          event.getActorLabel() == null ? event.getActorType().name() : event.getActorLabel(),
          event.getCorrelationId(),
          event.getEventHash(),
          event.getMetadata(),
          event.getOccurredAt());
    }
  }
}
