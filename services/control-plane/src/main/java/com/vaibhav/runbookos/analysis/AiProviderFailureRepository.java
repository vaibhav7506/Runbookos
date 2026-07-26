package com.vaibhav.runbookos.analysis;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiProviderFailureRepository extends JpaRepository<AiProviderFailure, UUID> {
  Optional<AiProviderFailure> findByOrganizationIdAndProvider(
      UUID organizationId, AiProvider provider);
}
