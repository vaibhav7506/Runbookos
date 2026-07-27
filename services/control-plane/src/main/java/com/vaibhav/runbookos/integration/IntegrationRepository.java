package com.vaibhav.runbookos.integration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Every finder is organization-scoped. There is deliberately no {@code findById(UUID)} usage in the
 * service layer: callers must supply the tenant, so a guessed identifier from another organization
 * returns empty rather than data.
 */
public interface IntegrationRepository extends JpaRepository<Integration, UUID> {

  Optional<Integration> findByIdAndOrganizationId(UUID id, UUID organizationId);

  List<Integration> findByOrganizationIdOrderByNameAsc(UUID organizationId);

  List<Integration> findByOrganizationIdAndKind(UUID organizationId, IntegrationKind kind);

  boolean existsByOrganizationIdAndName(UUID organizationId, String name);

  long countByOrganizationIdAndStatusIn(
      UUID organizationId, java.util.Collection<IntegrationStatus> statuses);
}
