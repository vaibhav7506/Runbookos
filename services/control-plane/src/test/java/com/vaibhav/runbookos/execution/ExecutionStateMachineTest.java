package com.vaibhav.runbookos.execution;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vaibhav.runbookos.exception.ConflictException;
import org.junit.jupiter.api.Test;

class ExecutionStateMachineTest {
  @Test
  void permitsApprovalPauseAndResume() {
    assertThatCode(
            () -> {
              ExecutionStateMachine.require(ExecutionStatus.QUEUED, ExecutionStatus.RUNNING);
              ExecutionStateMachine.require(
                  ExecutionStatus.RUNNING, ExecutionStatus.PAUSED_FOR_APPROVAL);
              ExecutionStateMachine.require(
                  ExecutionStatus.PAUSED_FOR_APPROVAL, ExecutionStatus.RUNNING);
              ExecutionStateMachine.require(ExecutionStatus.RUNNING, ExecutionStatus.SUCCEEDED);
            })
        .doesNotThrowAnyException();
  }

  @Test
  void terminalSuccessCannotBeReopened() {
    assertThatThrownBy(
            () -> ExecutionStateMachine.require(ExecutionStatus.SUCCEEDED, ExecutionStatus.RUNNING))
        .isInstanceOf(ConflictException.class);
  }
}
