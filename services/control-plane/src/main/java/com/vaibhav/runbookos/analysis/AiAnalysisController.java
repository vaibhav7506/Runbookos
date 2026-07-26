package com.vaibhav.runbookos.analysis;

import com.vaibhav.runbookos.exception.AccessDeniedDomainException;
import com.vaibhav.runbookos.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AiAnalysisController {
  private final AiAnalysisService service;

  public AiAnalysisController(AiAnalysisService service) {
    this.service = service;
  }

  @PostMapping("/incidents/{incidentId}/analyses")
  @ResponseStatus(HttpStatus.CREATED)
  public AiAnalysisService.AnalysisView analyze(
      @PathVariable UUID incidentId, @AuthenticationPrincipal AuthenticatedUser user) {
    return service.analyze(requireOrg(user), incidentId, user.userId());
  }

  @GetMapping("/incidents/{incidentId}/analyses")
  public List<AiAnalysisService.AnalysisView> list(
      @PathVariable UUID incidentId, @AuthenticationPrincipal AuthenticatedUser user) {
    return service.list(requireOrg(user), incidentId, user.userId());
  }

  @PostMapping("/ai/providers")
  @ResponseStatus(HttpStatus.CREATED)
  public AiAnalysisService.ProviderConfigView configure(
      @Valid @RequestBody ProviderRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return service.configure(
        requireOrg(user),
        new AiAnalysisService.ConfigureProviderRequest(
            request.provider(),
            request.model(),
            request.baseUrl(),
            request.apiKey(),
            request.priority(),
            request.timeoutSeconds(),
            request.maxRetries(),
            request.inputTokenBudget(),
            request.outputTokenBudget(),
            request.costBudgetUsd()),
        user.userId());
  }

  @GetMapping("/ai/providers")
  public List<AiAnalysisService.ProviderConfigView> providers(
      @AuthenticationPrincipal AuthenticatedUser user) {
    return service.providerConfigs(requireOrg(user), user.userId());
  }

  @DeleteMapping("/ai/providers/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disable(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
    service.disableProvider(requireOrg(user), id, user.userId());
  }

  private static UUID requireOrg(AuthenticatedUser user) {
    if (!user.hasOrganization())
      throw new AccessDeniedDomainException("Select an organization first");
    return user.organizationId();
  }

  public record ProviderRequest(
      @NotNull AiProvider provider,
      @NotBlank String model,
      String baseUrl,
      @NotBlank String apiKey,
      @Min(1) int priority,
      @Min(1) @Max(120) int timeoutSeconds,
      @Min(0) @Max(3) int maxRetries,
      @Min(256) int inputTokenBudget,
      @Min(128) int outputTokenBudget,
      @DecimalMin("0.0") BigDecimal costBudgetUsd) {}
}
