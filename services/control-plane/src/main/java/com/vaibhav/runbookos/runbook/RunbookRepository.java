package com.vaibhav.runbookos.runbook;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunbookRepository extends JpaRepository<Runbook, UUID> {
  List<Runbook> findByOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);

  Optional<Runbook> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
