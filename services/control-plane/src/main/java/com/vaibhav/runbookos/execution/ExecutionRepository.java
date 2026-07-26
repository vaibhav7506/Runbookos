package com.vaibhav.runbookos.execution;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRepository extends JpaRepository<Execution, UUID> {
  Optional<Execution> findByIdAndOrganizationId(UUID id, UUID org);

  Optional<Execution> findByOrganizationIdAndIdempotencyKey(UUID org, String key);

  List<Execution> findByOrganizationIdAndIncidentIdOrderByCreatedAtAsc(UUID org, UUID incident);

  List<Execution> findByStatusInAndTimeoutAtBefore(
      Collection<ExecutionStatus> status, Instant before);
}
