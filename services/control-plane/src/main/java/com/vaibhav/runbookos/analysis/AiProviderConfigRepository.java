package com.vaibhav.runbookos.analysis;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiProviderConfigRepository extends JpaRepository<AiProviderConfig, UUID> {
  List<AiProviderConfig> findByOrganizationIdAndEnabledTrueOrderByPriorityAsc(UUID organizationId);

  List<AiProviderConfig> findByOrganizationIdOrderByPriorityAsc(UUID organizationId);

  Optional<AiProviderConfig> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
