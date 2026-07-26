package com.vaibhav.runbookos.organization;

import com.vaibhav.runbookos.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
  private final OrganizationService service;

  public OrganizationController(OrganizationService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrganizationResponse create(
      @Valid @RequestBody CreateOrganizationRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return OrganizationResponse.from(
        service.create(request.name(), request.slug(), request.demoMode(), user.userId()));
  }

  @PostMapping("/{organizationId}/switch")
  public SwitchOrganizationResponse switchOrganization(
      @PathVariable UUID organizationId, @AuthenticationPrincipal AuthenticatedUser user) {
    return new SwitchOrganizationResponse(
        service.switchTo(organizationId, user.userId()), organizationId);
  }

  public record CreateOrganizationRequest(
      @NotBlank @Size(max = 120) String name, @Size(max = 64) String slug, boolean demoMode) {}

  public record SwitchOrganizationResponse(String accessToken, UUID organizationId) {}

  public record OrganizationResponse(UUID id, String name, String slug, boolean demoMode) {
    static OrganizationResponse from(Organization organization) {
      return new OrganizationResponse(
          organization.getId(),
          organization.getName(),
          organization.getSlug(),
          organization.isDemoMode());
    }
  }
}
