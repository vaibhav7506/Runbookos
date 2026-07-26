package com.vaibhav.runbookos.runbook;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunbookStepRepository extends JpaRepository<RunbookStep, UUID> {
  List<RunbookStep> findByRunbookVersionIdOrderBySequenceNumberAsc(UUID versionId);

  void deleteByRunbookVersionId(UUID versionId);
}
