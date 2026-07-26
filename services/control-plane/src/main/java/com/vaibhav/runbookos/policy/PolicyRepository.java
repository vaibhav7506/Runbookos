package com.vaibhav.runbookos.policy;

import com.vaibhav.runbookos.integration.IntegrationEnvironment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {
  Optional<Policy> findByOrganizationIdAndEnvironmentAndEnabledTrue(
      UUID organizationId, IntegrationEnvironment environment);

  List<Policy> findByOrganizationIdOrderByEnvironmentAsc(UUID organizationId);

  Optional<Policy> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
