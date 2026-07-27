package com.vaibhav.runbookos.operations;

import com.vaibhav.runbookos.analysis.IncidentAnalysisRepository;
import com.vaibhav.runbookos.approval.*;
import com.vaibhav.runbookos.exception.AccessDeniedDomainException;
import com.vaibhav.runbookos.execution.*;
import com.vaibhav.runbookos.incident.*;
import com.vaibhav.runbookos.integration.*;
import com.vaibhav.runbookos.organization.TenantAccessService;
import com.vaibhav.runbookos.security.AuthenticatedUser;
import com.vaibhav.runbookos.workflow.*;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations")
public class OperationsController {
  private final IncidentRepository incidents;
  private final ApprovalRequestRepository approvals;
  private final ExecutionRepository executions;
  private final IntegrationRepository integrations;
  private final OutboxEventRepository outbox;
  private final IncidentAnalysisRepository analyses;
  private final TenantAccessService access;

  public OperationsController(
      IncidentRepository incidents,
      ApprovalRequestRepository approvals,
      ExecutionRepository executions,
      IntegrationRepository integrations,
      OutboxEventRepository outbox,
      IncidentAnalysisRepository analyses,
      TenantAccessService access) {
    this.incidents = incidents;
    this.approvals = approvals;
    this.executions = executions;
    this.integrations = integrations;
    this.outbox = outbox;
    this.analyses = analyses;
    this.access = access;
  }

  @GetMapping("/overview")
  public OperationsOverview overview(@AuthenticationPrincipal AuthenticatedUser user) {
    UUID org = requireOrg(user);
    access.require(org, user.userId());
    long active =
        incidents.countByOrganizationIdAndStatusNotIn(
            org, List.of(IncidentStatus.RESOLVED, IncidentStatus.CLOSED, IncidentStatus.CANCELLED));
    long pending = approvals.countByOrganizationIdAndStatus(org, ApprovalStatus.PENDING);
    long unhealthy =
        integrations.countByOrganizationIdAndStatusIn(
            org, List.of(IntegrationStatus.ERROR, IntegrationStatus.DEGRADED));
    long running =
        executions.countByOrganizationIdAndStatusIn(
            org,
            List.of(
                ExecutionStatus.QUEUED,
                ExecutionStatus.RUNNING,
                ExecutionStatus.PAUSED_FOR_APPROVAL));
    long dead = outbox.countByOrganizationIdAndStatus(org, OutboxStatus.DEAD);
    List<RecentExecution> recent =
        executions.findByOrganizationIdOrderByCreatedAtDesc(org, PageRequest.of(0, 5)).stream()
            .map(
                value ->
                    new RecentExecution(
                        value.getId(),
                        value.getIncidentId(),
                        value.getWorkflowKey(),
                        value.getStatus().name(),
                        value.getCreatedAt()))
            .toList();
    return new OperationsOverview(
        dead == 0 ? "OPERATIONAL" : "ATTENTION_REQUIRED",
        Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()).toString(),
        active,
        pending,
        running,
        unhealthy,
        dead,
        Optional.ofNullable(analyses.totalTokens(org)).orElse(0L),
        Optional.ofNullable(analyses.totalCost(org)).orElse(BigDecimal.ZERO),
        recent);
  }

  private static UUID requireOrg(AuthenticatedUser user) {
    if (!user.hasOrganization())
      throw new AccessDeniedDomainException("Select an organization first");
    return user.organizationId();
  }

  public record OperationsOverview(
      String systemStatus,
      String uptime,
      long activeIncidents,
      long pendingApprovals,
      long activeExecutions,
      long unhealthyIntegrations,
      long deadLetters,
      long aiTokens,
      BigDecimal estimatedAiCostUsd,
      List<RecentExecution> recentExecutions) {}

  public record RecentExecution(
      UUID id, UUID incidentId, String workflowKey, String status, java.time.Instant createdAt) {}
}
