package com.vaibhav.runbookos.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaibhav.runbookos.audit.AuditActions;
import com.vaibhav.runbookos.audit.AuditService;
import com.vaibhav.runbookos.exception.AccessDeniedDomainException;
import com.vaibhav.runbookos.identity.Role;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantAccessServiceTest {
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final AuditService audit = mock(AuditService.class);
  private final TenantAccessService service = new TenantAccessService(memberships, audit);

  @Test
  void returnsAuthoritativeActiveMembership() {
    UUID organization = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    Membership membership =
        Membership.create(
            organization, user, Role.RESPONDER, Instant.parse("2026-07-26T00:00:00Z"));
    when(memberships.findByOrganizationIdAndUserId(organization, user))
        .thenReturn(Optional.of(membership));

    assertThat(service.require(organization, user, Role.RESPONDER)).isEqualTo(membership);
  }

  @Test
  void blocksCrossTenantLookupAndAuditsTheDenial() {
    UUID requestedOrganization = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    when(memberships.findByOrganizationIdAndUserId(requestedOrganization, user))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.require(requestedOrganization, user))
        .isInstanceOf(AccessDeniedDomainException.class);
    verify(audit)
        .recordDenied(
            AuditActions.ACCESS_DENIED,
            "organization",
            requestedOrganization,
            user,
            "Tenant role does not permit this operation");
  }

  @Test
  void viewerCannotUseResponderOperation() {
    UUID organization = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    Membership membership =
        Membership.create(organization, user, Role.VIEWER, Instant.parse("2026-07-26T00:00:00Z"));
    when(memberships.findByOrganizationIdAndUserId(organization, user))
        .thenReturn(Optional.of(membership));

    assertThatThrownBy(
            () -> service.require(organization, user, Role.OWNER, Role.ADMIN, Role.RESPONDER))
        .isInstanceOf(AccessDeniedDomainException.class);
  }
}
