package com.vaibhav.runbookos.execution;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StepExecutionRepository extends JpaRepository<StepExecution, UUID> {
  List<StepExecution> findByExecutionIdOrderBySequenceNumberAscAttemptAsc(UUID executionId);

  Optional<StepExecution> findFirstByExecutionIdAndActionIdOrderByAttemptDesc(
      UUID executionId, String actionId);
}
