package com.vaibhav.runbookos.execution;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionEventRepository extends JpaRepository<ExecutionEvent, UUID> {
  List<ExecutionEvent> findByExecutionIdOrderByOccurredAtAscIdAsc(UUID executionId);
}
