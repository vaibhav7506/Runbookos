package com.vaibhav.runbookos.incident;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentPostmortemRepository extends JpaRepository<IncidentPostmortem, UUID> {
  Optional<IncidentPostmortem> findByOrganizationIdAndIncidentId(
      UUID organizationId, UUID incidentId);
}
