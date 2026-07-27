package com.vaibhav.runbookos.integration;

import com.vaibhav.runbookos.exception.AccessDeniedDomainException;
import com.vaibhav.runbookos.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {
  private final IntegrationService service;

  public IntegrationController(IntegrationService service) {
    this.service = service;
  }

  @GetMapping
  public List<IntegrationService.IntegrationView> list(
      @AuthenticationPrincipal AuthenticatedUser user) {
    return service.list(requireOrg(user), user.userId());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public IntegrationService.IntegrationView create(
      @Valid @RequestBody SetupRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
    return service.create(
        requireOrg(user),
        new IntegrationService.SetupRequest(
            request.kind(),
            request.name(),
            request.environment(),
            request.config(),
            request.secret()),
        user.userId());
  }

  @PostMapping("/{id}/validate")
  public IntegrationService.IntegrationView validate(
      @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
    return service.validate(requireOrg(user), id, user.userId());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disconnect(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
    service.disconnect(requireOrg(user), id, user.userId());
  }

  @GetMapping("/{id}/usage")
  public List<IntegrationService.UsageView> usage(
      @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
    return service.recentUsage(requireOrg(user), id, user.userId());
  }

  private static UUID requireOrg(AuthenticatedUser user) {
    if (!user.hasOrganization())
      throw new AccessDeniedDomainException("Select an organization first");
    return user.organizationId();
  }

  public record SetupRequest(
      @NotNull IntegrationKind kind,
      @NotBlank @Size(max = 120) String name,
      @NotNull IntegrationEnvironment environment,
      Map<String, Object> config,
      @Size(max = 4096) String secret) {}
}
