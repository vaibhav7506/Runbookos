package com.vaibhav.runbookos.runbook;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunbookVersionRepository extends JpaRepository<RunbookVersion, UUID> {
  List<RunbookVersion> findByRunbookIdOrderByVersionNumberDesc(UUID runbookId);

  Optional<RunbookVersion> findFirstByRunbookIdAndStatusOrderByVersionNumberDesc(
      UUID runbookId, RunbookVersionStatus status);
}
