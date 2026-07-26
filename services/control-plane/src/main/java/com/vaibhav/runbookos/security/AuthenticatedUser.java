package com.vaibhav.runbookos.security;

import com.vaibhav.runbookos.identity.Role;
import java.security.Principal;
import java.util.UUID;

/**
 * Verified access-token identity. Tenant and role are absent before an organization is selected.
 */
public record AuthenticatedUser(UUID userId, UUID organizationId, Role role) implements Principal {
  @Override
  public String getName() {
    return userId.toString();
  }

  public boolean hasOrganization() {
    return organizationId != null && role != null;
  }
}
