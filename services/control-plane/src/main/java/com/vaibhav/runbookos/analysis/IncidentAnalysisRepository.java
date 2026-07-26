package com.vaibhav.runbookos.analysis;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentAnalysisRepository extends JpaRepository<IncidentAnalysis, UUID> {
  List<IncidentAnalysis> findByOrganizationIdAndIncidentIdOrderByCreatedAtDesc(
      UUID organizationId, UUID incidentId);
}
