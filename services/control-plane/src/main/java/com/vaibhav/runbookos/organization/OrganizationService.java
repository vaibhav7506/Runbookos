package com.vaibhav.runbookos.organization;

import com.vaibhav.runbookos.audit.AuditActions;
import com.vaibhav.runbookos.audit.AuditRecord;
import com.vaibhav.runbookos.audit.AuditService;
import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.ConflictException;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.security.JwtService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {
  private final OrganizationRepository organizations;
  private final MembershipRepository memberships;
  private final TenantAccessService access;
  private final AuditService audit;
  private final JwtService jwt;
  private final TimeProvider time;

  public OrganizationService(
      OrganizationRepository organizations,
      MembershipRepository memberships,
      TenantAccessService access,
      AuditService audit,
      JwtService jwt,
      TimeProvider time) {
    this.organizations = organizations;
    this.memberships = memberships;
    this.access = access;
    this.audit = audit;
    this.jwt = jwt;
    this.time = time;
  }

  @Transactional
  public Organization create(String name, String requestedSlug, boolean demoMode, UUID userId) {
    String slug =
        requestedSlug == null || requestedSlug.isBlank()
            ? Organization.slugify(name)
            : Organization.slugify(requestedSlug);
    if (organizations.existsBySlug(slug)) {
      throw new ConflictException(
          "ORGANIZATION_SLUG_EXISTS", "That organization URL is already in use");
    }
    Instant now = time.nowTruncated();
    Organization organization =
        organizations.save(Organization.create(name.trim(), slug, demoMode, userId, now));
    memberships.save(Membership.create(organization.getId(), userId, Role.OWNER, now));
    audit.record(
        AuditRecord.builder(AuditActions.ORGANIZATION_CREATED, "organization")
            .organization(organization.getId())
            .actor(userId, null)
            .resourceId(organization.getId())
            .metadata("demoMode", demoMode)
            .build());
    return organization;
  }

  @Transactional(readOnly = true)
  public String switchTo(UUID organizationId, UUID userId) {
    Membership membership = access.require(organizationId, userId);
    audit.recordSuccess(
        AuditActions.ORGANIZATION_SWITCHED, "organization", organizationId, userId, organizationId);
    return jwt.issue(userId, organizationId, membership.getRole(), time.now());
  }
}
