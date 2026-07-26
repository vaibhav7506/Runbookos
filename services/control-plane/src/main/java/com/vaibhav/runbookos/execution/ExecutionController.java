package com.vaibhav.runbookos.execution;

import com.vaibhav.runbookos.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {
  private final ExecutionService service;
  private final ExecutionEventStream stream;

  public ExecutionController(ExecutionService service, ExecutionEventStream stream) {
    this.service = service;
    this.stream = stream;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ExecutionService.ExecutionView create(
      @Valid @RequestBody CreateExecutionRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return ExecutionService.ExecutionView.from(
        service.create(
            requireOrg(user),
            request.incidentId(),
            request.workflowKey(),
            idempotencyKey,
            user.userId()));
  }

  @GetMapping("/{id}")
  public ExecutionService.ExecutionTimeline timeline(
      @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
    return service.timeline(requireOrg(user), id, user.userId());
  }

  @PostMapping("/{id}/retry")
  public ExecutionService.ExecutionView retry(
      @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
    return ExecutionService.ExecutionView.from(service.retry(requireOrg(user), id, user.userId()));
  }

  @PostMapping("/{id}/steps/{actionId}/retry")
  public ExecutionService.StepView retryStep(
      @PathVariable UUID id,
      @PathVariable String actionId,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return ExecutionService.StepView.from(
        service.retryStep(requireOrg(user), id, actionId, user.userId()));
  }

  @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter events(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user)
      throws IOException {
    ExecutionService.ExecutionTimeline timeline =
        service.timeline(requireOrg(user), id, user.userId());
    SseEmitter emitter = stream.subscribe(id);
    emitter.send(SseEmitter.event().name("snapshot").data(timeline));
    return emitter;
  }

  private static UUID requireOrg(AuthenticatedUser user) {
    if (!user.hasOrganization())
      throw new com.vaibhav.runbookos.exception.AccessDeniedDomainException(
          "Select an organization first");
    return user.organizationId();
  }

  public record CreateExecutionRequest(UUID incidentId, @NotBlank String workflowKey) {}
}
