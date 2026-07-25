package com.vaibhav.runbookos.integration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationCredentialReferenceRepository
    extends JpaRepository<IntegrationCredentialReference, UUID> {

  Optional<IntegrationCredentialReference> findByIntegrationIdAndCredentialKey(
      UUID integrationId, String credentialKey);

  List<IntegrationCredentialReference> findByIntegrationId(UUID integrationId);

  List<IntegrationCredentialReference> findByOrganizationId(UUID organizationId);
}
