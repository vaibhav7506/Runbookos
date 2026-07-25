package com.vaibhav.runbookos.organization;

import com.vaibhav.runbookos.identity.Role;
import java.util.UUID;

/**
 * A user's membership joined to its organization. Returned by a single query so the organization
 * switcher and current-user endpoint do not issue one organization lookup per membership.
 */
public record OrganizationMembershipView(
    UUID organizationId,
    String organizationName,
    String organizationSlug,
    boolean demoMode,
    Role role,
    MembershipStatus status) {}
