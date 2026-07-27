package com.vaibhav.runbookos.workflow;

import com.vaibhav.runbookos.exception.AccessDeniedDomainException;
import com.vaibhav.runbookos.security.AuthenticatedUser;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations/dead-letters")
public class DeadLetterController {
  private final DeadLetterService service;

  public DeadLetterController(DeadLetterService service) {
    this.service = service;
  }

  @GetMapping
  public List<DeadLetterService.DeadLetterView> list(
      @RequestParam(defaultValue = "25") int size,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return service.list(requireOrg(user), user.userId(), size);
  }

  @PostMapping("/{id}/redrive")
  public DeadLetterService.DeadLetterView redrive(
      @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
    return service.redrive(requireOrg(user), id, user.userId());
  }

  private static UUID requireOrg(AuthenticatedUser user) {
    if (!user.hasOrganization())
      throw new AccessDeniedDomainException("Select an organization first");
    return user.organizationId();
  }
}
