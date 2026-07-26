package com.vaibhav.runbookos.execution;

import com.vaibhav.runbookos.exception.ConflictException;
import java.util.*;

public final class ExecutionStateMachine {
  private static final Map<ExecutionStatus, Set<ExecutionStatus>> ALLOWED =
      Map.of(
          ExecutionStatus.QUEUED,
          Set.of(ExecutionStatus.RUNNING, ExecutionStatus.CANCELLED, ExecutionStatus.TIMED_OUT),
          ExecutionStatus.RUNNING,
          Set.of(
              ExecutionStatus.PAUSED_FOR_APPROVAL,
              ExecutionStatus.SUCCEEDED,
              ExecutionStatus.PARTIALLY_SUCCEEDED,
              ExecutionStatus.FAILED,
              ExecutionStatus.TIMED_OUT,
              ExecutionStatus.CANCELLED),
          ExecutionStatus.PAUSED_FOR_APPROVAL,
          Set.of(
              ExecutionStatus.RUNNING,
              ExecutionStatus.FAILED,
              ExecutionStatus.CANCELLED,
              ExecutionStatus.TIMED_OUT),
          ExecutionStatus.FAILED,
          Set.of(ExecutionStatus.QUEUED),
          ExecutionStatus.SUCCEEDED,
          Set.of(),
          ExecutionStatus.PARTIALLY_SUCCEEDED,
          Set.of(ExecutionStatus.QUEUED),
          ExecutionStatus.TIMED_OUT,
          Set.of(ExecutionStatus.QUEUED),
          ExecutionStatus.CANCELLED,
          Set.of());

  private ExecutionStateMachine() {}

  public static void require(ExecutionStatus from, ExecutionStatus to) {
    if (!ALLOWED.getOrDefault(from, Set.of()).contains(to))
      throw new ConflictException(
          "INVALID_EXECUTION_TRANSITION", "Execution cannot transition from " + from + " to " + to);
  }
}
