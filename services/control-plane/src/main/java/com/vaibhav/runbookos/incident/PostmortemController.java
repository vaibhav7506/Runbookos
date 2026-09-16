package com.vaibhav.runbookos.incident;

import com.vaibhav.runbookos.exception.*;
import com.vaibhav.runbookos.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents/{incidentId}/postmortem")
public class PostmortemController {
  private final PostmortemService service;

  public PostmortemController(PostmortemService service) {
    this.service = service;
  }

  @GetMapping
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "Existing postmortem",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              schema =
                  @io.swagger.v3.oas.annotations.media.Schema(
                      implementation = PostmortemService.PostmortemView.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "204",
      description = "No postmortem has been generated yet",
      content = @io.swagger.v3.oas.annotations.media.Content)
  public ResponseEntity<PostmortemService.PostmortemView> find(
      @PathVariable UUID incidentId, @AuthenticationPrincipal AuthenticatedUser user) {
    return service
        .find(requireOrg(user), incidentId, user.userId())
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PostmortemService.PostmortemView generate(
      @PathVariable UUID incidentId, @AuthenticationPrincipal AuthenticatedUser user) {
    return service.generate(requireOrg(user), incidentId, user.userId());
  }

  private static UUID requireOrg(AuthenticatedUser user) {
    if (!user.hasOrganization())
      throw new AccessDeniedDomainException("Select an organization first");
    return user.organizationId();
  }
}
