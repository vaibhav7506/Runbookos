package com.vaibhav.runbookos.organization;

import com.vaibhav.runbookos.audit.AuditActions;
import com.vaibhav.runbookos.audit.AuditService;
import com.vaibhav.runbookos.exception.AccessDeniedDomainException;
import com.vaibhav.runbookos.identity.Role;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Authoritative tenant authorization; token role claims are only a routing hint. */
@Service
public class TenantAccessService {
  private final MembershipRepository memberships;
  private final AuditService auditService;

  public TenantAccessService(MembershipRepository memberships, AuditService auditService) {
    this.memberships = memberships;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public Membership require(UUID organizationId, UUID userId, Role... allowed) {
    Membership membership =
        memberships.findByOrganizationIdAndUserId(organizationId, userId).orElse(null);
    boolean roleAllowed =
        membership != null
            && membership.isActive()
            && (allowed.length == 0 || Arrays.asList(allowed).contains(membership.getRole()));
    if (!roleAllowed) {
      auditService.recordDenied(
          AuditActions.ACCESS_DENIED,
          "organization",
          organizationId,
          userId,
          "Tenant role does not permit this operation");
      throw new AccessDeniedDomainException("Tenant role does not permit this operation");
    }
    return membership;
  }
}
