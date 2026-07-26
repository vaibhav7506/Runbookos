package com.vaibhav.runbookos.incident;

import com.vaibhav.runbookos.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
  private final IncidentService service;

  public IncidentController(IncidentService service) {
    this.service = service;
  }

  @GetMapping
  public IncidentService.IncidentPage search(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(required = false) IncidentStatus status,
      @RequestParam(required = false) IncidentSeverity severity,
      @RequestParam(required = false) String serviceName,
      @RequestParam(required = false, name = "q") String query,
      @RequestParam(required = false) Instant cursor,
      @RequestParam(defaultValue = "25") int size) {
    return service.search(
        requireOrg(user), user.userId(), status, severity, serviceName, query, cursor, size);
  }

  @GetMapping("/{id}")
  public IncidentService.IncidentDetail detail(
      @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
    return service.detail(requireOrg(user), id, user.userId());
  }

  @PostMapping("/{id}/transitions")
  public IncidentService.IncidentView transition(
      @PathVariable UUID id,
      @Valid @RequestBody TransitionRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return IncidentService.IncidentView.from(
        service.transition(requireOrg(user), id, request.status(), user.userId()));
  }

  @PostMapping("/{id}/assignments")
  public IncidentService.IncidentView assign(
      @PathVariable UUID id,
      @Valid @RequestBody AssignRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return IncidentService.IncidentView.from(
        service.assign(requireOrg(user), id, request.assigneeUserId(), user.userId()));
  }

  @PostMapping("/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public IncidentService.CommentView comment(
      @PathVariable UUID id,
      @Valid @RequestBody CommentRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return IncidentService.CommentView.from(
        service.addComment(requireOrg(user), id, request.body(), user.userId()));
  }

  @PostMapping("/demo")
  @ResponseStatus(HttpStatus.CREATED)
  public IncidentService.IncidentView demo(@AuthenticationPrincipal AuthenticatedUser user) {
    Incident i =
        service.ingest(
            requireOrg(user),
            SignalSource.DEMO,
            "demo-" + UUID.randomUUID(),
            Map.of(
                "title",
                "Checkout error rate increased",
                "summary",
                "Seeded deployment regression for the guided demo.",
                "service",
                "checkout-api",
                "severity",
                "SEV2",
                "priority",
                2,
                "errorType",
                "DeploymentRegression"));
    return IncidentService.IncidentView.from(i);
  }

  private static UUID requireOrg(AuthenticatedUser user) {
    if (!user.hasOrganization())
      throw new com.vaibhav.runbookos.exception.AccessDeniedDomainException(
          "Select an organization first");
    return user.organizationId();
  }

  public record TransitionRequest(IncidentStatus status) {}

  public record AssignRequest(UUID assigneeUserId) {}

  public record CommentRequest(@NotBlank String body) {}
}
