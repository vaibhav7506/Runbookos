package com.vaibhav.runbookos.organization;

import com.vaibhav.runbookos.identity.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

  /** The authorization lookup: resolves whether a user may act within an organization at all. */
  Optional<Membership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

  List<Membership> findByUserId(UUID userId);

  List<Membership> findByOrganizationId(UUID organizationId);

  /**
   * Loads a user's active memberships together with their organizations in a single query. Used by
   * the current-user endpoint and the organization switcher, both of which would otherwise trigger
   * an N+1 over organizations.
   */
  @Query(
      """
      select new com.vaibhav.runbookos.organization.OrganizationMembershipView(
          o.id, o.name, o.slug, o.demoMode, m.role, m.status)
      from Membership m
      join Organization o on o.id = m.organizationId
      where m.userId = :userId
        and m.status = com.vaibhav.runbookos.organization.MembershipStatus.ACTIVE
      order by o.name asc
      """)
  List<OrganizationMembershipView> findActiveMembershipsWithOrganizations(
      @Param("userId") UUID userId);

  /** Guards removal of the last OWNER, which would leave an organization unadministrable. */
  long countByOrganizationIdAndRoleAndStatus(
      UUID organizationId, Role role, MembershipStatus status);
}
