package com.vaibhav.runbookos.integration;

import com.vaibhav.runbookos.audit.*;
import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.*;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.integration.dto.CredentialSummaryResponse;
import com.vaibhav.runbookos.organization.TenantAccessService;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.*;

/** Organization-isolated setup and health management for external integrations. */
@Service
public class IntegrationService {
  private static final Set<String> SECRET_CONFIG_KEYS =
      Set.of("token", "secret", "password", "apiKey", "signingSecret", "webhookSecret");

  private final IntegrationRepository integrations;
  private final IntegrationUsageEventRepository usage;
  private final IntegrationCredentialService credentials;
  private final TenantAccessService access;
  private final AuditService audit;
  private final TimeProvider time;

  public IntegrationService(
      IntegrationRepository integrations,
      IntegrationUsageEventRepository usage,
      IntegrationCredentialService credentials,
      TenantAccessService access,
      AuditService audit,
      TimeProvider time) {
    this.integrations = integrations;
    this.usage = usage;
    this.credentials = credentials;
    this.access = access;
    this.audit = audit;
    this.time = time;
  }

  @Transactional(readOnly = true)
  public List<IntegrationView> list(UUID org, UUID actor) {
    access.require(org, actor);
    return integrations.findByOrganizationIdOrderByNameAsc(org).stream().map(this::view).toList();
  }

  @Transactional
  public IntegrationView create(UUID org, SetupRequest request, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    if (integrations.existsByOrganizationIdAndName(org, request.name().trim())) {
      throw new ConflictException(
          "INTEGRATION_NAME_EXISTS", "An integration with this name already exists");
    }
    Map<String, Object> config = sanitizedConfig(request.config());
    Integration value =
        integrations.save(
            Integration.create(
                org,
                request.kind(),
                request.name().trim(),
                request.environment(),
                config,
                actor,
                time.nowTruncated()));
    if (request.secret() != null && !request.secret().isBlank()) {
      credentials.store(
          org, value.getId(), credentialKey(value.getKind()), request.secret(), actor);
    }
    audit.recordSuccess(AuditActions.INTEGRATION_CREATED, "integration", org, actor, value.getId());
    return view(value);
  }

  @Transactional
  public IntegrationView validate(UUID org, UUID id, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    Integration value = require(org, id);
    Instant now = time.nowTruncated();
    ValidationResult result;
    try {
      result = validateAdapter(org, value);
      if (result.degraded()) value.markDegraded(result.message(), now);
      else value.markConnected(now);
      recordUsage(
          value, "VALIDATE", result.degraded() ? "DEGRADED" : "SUCCEEDED", result.message());
    } catch (IntegrationValidationException ex) {
      value.markError(ex.getMessage(), now);
      recordUsage(value, "VALIDATE", "FAILED", ex.getMessage());
      throw ex;
    }
    audit.recordSuccess(AuditActions.INTEGRATION_VALIDATED, "integration", org, actor, id);
    return view(value);
  }

  @Transactional
  public void disconnect(UUID org, UUID id, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    Integration value = require(org, id);
    for (CredentialSummaryResponse secret : credentials.list(org, id)) {
      if (!secret.revoked()) credentials.revoke(org, id, secret.credentialKey(), actor);
    }
    value.disconnect(time.nowTruncated());
    recordUsage(value, "DISCONNECT", "SUCCEEDED", "Credential references revoked");
    audit.recordSuccess(AuditActions.INTEGRATION_DISCONNECTED, "integration", org, actor, id);
  }

  @Transactional(readOnly = true)
  public List<UsageView> recentUsage(UUID org, UUID id, UUID actor) {
    access.require(org, actor);
    require(org, id);
    return usage
        .findByOrganizationIdAndIntegrationIdOrderByOccurredAtDesc(org, id, PageRequest.of(0, 20))
        .stream()
        .map(e -> new UsageView(e.getOperation(), e.getOutcome(), e.getDetail(), e.getOccurredAt()))
        .toList();
  }

  private ValidationResult validateAdapter(UUID org, Integration value) {
    boolean demo = Boolean.TRUE.equals(value.getConfig().get("demo"));
    if (demo) {
      return new ValidationResult(
          false,
          "Demo adapter ready; no external request was made",
          demoCapabilities(value.getKind()));
    }
    if (value.getKind() != IntegrationKind.GITHUB) {
      throw new IntegrationValidationException(
          "REAL_ADAPTER_NOT_CONFIGURED",
          "This connector is currently an honest demo adapter. Enable Demo adapter or configure GitHub for live validation.");
    }
    return validateGitHub(org, value);
  }

  @SuppressWarnings("unchecked")
  private ValidationResult validateGitHub(UUID org, Integration value) {
    String token;
    try {
      token = credentials.revealForUse(org, value.getId(), "token");
    } catch (ResourceNotFoundException ex) {
      throw new IntegrationValidationException(
          "GITHUB_TOKEN_MISSING",
          "Add a GitHub token with metadata permission; repository features need Contents read access.");
    }
    String repository = Objects.toString(value.getConfig().get("repository"), "").trim();
    if (!repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
      throw new IntegrationValidationException(
          "GITHUB_REPOSITORY_INVALID", "Use the repository scope owner/name.");
    }
    try {
      RestClient client =
          RestClient.builder()
              .baseUrl("https://api.github.com")
              .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
              .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
              .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
              .build();
      ResponseEntity<Map> response =
          client.get().uri("/repos/{repository}", repository).retrieve().toEntity(Map.class);
      String remaining =
          Optional.ofNullable(response.getHeaders().getFirst("X-RateLimit-Remaining"))
              .orElse("unknown");
      Map<String, Object> body = response.getBody() == null ? Map.of() : response.getBody();
      boolean canIssues = Boolean.TRUE.equals(body.get("has_issues"));
      String message =
          canIssues
              ? "Connected to " + repository + "; rate limit remaining " + remaining
              : "Repository metadata works, but Issues are disabled; issue creation will be skipped";
      return new ValidationResult(
          !canIssues,
          message,
          canIssues
              ? List.of("metadata", "commits", "deployments", "pull_requests", "issues")
              : List.of("metadata", "commits", "deployments", "pull_requests"));
    } catch (HttpClientErrorException.Forbidden ex) {
      throw new IntegrationValidationException(
          "GITHUB_PERMISSION_MISSING",
          "GitHub denied the request. Grant Metadata read and Contents read permissions; Issues write is optional.");
    } catch (HttpClientErrorException.Unauthorized ex) {
      throw new IntegrationValidationException(
          "GITHUB_TOKEN_INVALID", "GitHub rejected the token. Rotate it and validate again.");
    } catch (HttpClientErrorException.NotFound ex) {
      throw new IntegrationValidationException(
          "GITHUB_REPOSITORY_NOT_FOUND", "Repository was not found or the token cannot access it.");
    } catch (RestClientException ex) {
      throw new IntegrationValidationException(
          "GITHUB_UNAVAILABLE", "GitHub validation could not complete. Try again later.");
    }
  }

  private IntegrationView view(Integration value) {
    List<CredentialSummaryResponse> refs =
        credentials.list(value.getOrganizationId(), value.getId());
    return new IntegrationView(
        value.getId(),
        value.getKind(),
        value.getName(),
        value.getEnvironment(),
        value.getStatus(),
        value.getConfig(),
        permissionExplanation(value.getKind()),
        refs,
        value.getLastSuccessAt(),
        value.getLastErrorAt(),
        value.getLastErrorMessage(),
        value.getUpdatedAt());
  }

  private static Map<String, Object> sanitizedConfig(Map<String, Object> source) {
    if (source == null) return Map.of();
    Map<String, Object> safe = new LinkedHashMap<>();
    source.forEach(
        (key, value) -> {
          if (!SECRET_CONFIG_KEYS.contains(key) && value != null) safe.put(key, value);
        });
    return Map.copyOf(safe);
  }

  private void recordUsage(Integration value, String operation, String outcome, String detail) {
    usage.save(
        IntegrationUsageEvent.create(
            value.getOrganizationId(),
            value.getId(),
            operation,
            outcome,
            detail,
            null,
            time.nowTruncated()));
  }

  private Integration require(UUID org, UUID id) {
    return integrations
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("integration", id));
  }

  private static String credentialKey(IntegrationKind kind) {
    return switch (kind) {
      case CUSTOM_WEBHOOK, SENTRY -> "signing_secret";
      case EMAIL -> "password";
      default -> "token";
    };
  }

  private static String permissionExplanation(IntegrationKind kind) {
    return switch (kind) {
      case GITHUB ->
          "Metadata and Contents read; Pull requests read; Deployments read when available; Issues write only for approved issue actions.";
      case SLACK ->
          "chat:write for notifications; interactivity callback signature verification for decisions.";
      case EMAIL -> "Send-only credentials. RunbookOS never reads a mailbox.";
      case JIRA -> "Project browse and issue create; comments are optional.";
      case SENTRY -> "Signed webhook ingestion only; no Sentry account access is required.";
      case CUSTOM_WEBHOOK ->
          "HMAC signing secret; outbound destinations must be explicitly allowlisted.";
      case HTTP_HEALTHCHECK -> "GET/HEAD only to allowlisted public health endpoints.";
    };
  }

  private static List<String> demoCapabilities(IntegrationKind kind) {
    return switch (kind) {
      case GITHUB -> List.of("metadata", "commits", "deployments", "pull_requests", "issues");
      case SLACK ->
          List.of("incident_notification", "approval_request", "decision_callback", "resolution");
      case EMAIL -> List.of("approval", "digest", "resolution");
      case JIRA -> List.of("issue_create", "comment");
      case SENTRY -> List.of("signed_ingest");
      case CUSTOM_WEBHOOK -> List.of("signed_ingest", "signed_delivery");
      case HTTP_HEALTHCHECK -> List.of("allowlisted_get", "allowlisted_head");
    };
  }

  public record SetupRequest(
      IntegrationKind kind,
      String name,
      IntegrationEnvironment environment,
      Map<String, Object> config,
      String secret) {}

  public record IntegrationView(
      UUID id,
      IntegrationKind kind,
      String name,
      IntegrationEnvironment environment,
      IntegrationStatus status,
      Map<String, Object> config,
      String permissionExplanation,
      List<CredentialSummaryResponse> credentials,
      Instant lastSuccessAt,
      Instant lastErrorAt,
      String lastErrorMessage,
      Instant updatedAt) {}

  public record UsageView(String operation, String outcome, String detail, Instant occurredAt) {}

  private record ValidationResult(boolean degraded, String message, List<String> capabilities) {}

  static final class IntegrationValidationException extends DomainException {
    IntegrationValidationException(String code, String message) {
      super(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }
}
