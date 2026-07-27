package com.vaibhav.runbookos.integration;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationUsageEventRepository
    extends JpaRepository<IntegrationUsageEvent, UUID> {
  List<IntegrationUsageEvent> findByOrganizationIdAndIntegrationIdOrderByOccurredAtDesc(
      UUID organizationId, UUID integrationId, Pageable pageable);
}
