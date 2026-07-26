package com.vaibhav.runbookos.incident;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentEvidenceRepository extends JpaRepository<IncidentEvidence, UUID> {
  List<IncidentEvidence> findByIncidentIdOrderByCollectedAtAsc(UUID incidentId);
}
