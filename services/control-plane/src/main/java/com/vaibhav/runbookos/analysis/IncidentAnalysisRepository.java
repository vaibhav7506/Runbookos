package com.vaibhav.runbookos.analysis;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentAnalysisRepository extends JpaRepository<IncidentAnalysis, UUID> {
  List<IncidentAnalysis> findByOrganizationIdAndIncidentIdOrderByCreatedAtDesc(
      UUID organizationId, UUID incidentId);

  @Query(
      "select coalesce(sum(a.inputTokens + a.outputTokens), 0) from IncidentAnalysis a where a.organizationId=:org")
  Long totalTokens(@Param("org") UUID organizationId);

  @Query(
      "select coalesce(sum(a.estimatedCostUsd), 0) from IncidentAnalysis a where a.organizationId=:org")
  BigDecimal totalCost(@Param("org") UUID organizationId);
}
