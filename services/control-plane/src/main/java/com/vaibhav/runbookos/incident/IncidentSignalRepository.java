package com.vaibhav.runbookos.incident;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentSignalRepository extends JpaRepository<IncidentSignal, UUID> {
  Optional<IncidentSignal> findByOrganizationIdAndSourceAndExternalId(
      UUID org, SignalSource source, String externalId);

  List<IncidentSignal> findByIncidentIdOrderByReceivedAtAsc(UUID incidentId);
}
