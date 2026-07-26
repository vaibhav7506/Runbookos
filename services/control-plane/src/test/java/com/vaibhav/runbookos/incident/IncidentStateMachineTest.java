package com.vaibhav.runbookos.incident;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vaibhav.runbookos.exception.ConflictException;
import org.junit.jupiter.api.Test;

class IncidentStateMachineTest {
  @Test
  void acceptsAValidResponseLifecycle() {
    assertThatCode(
            () -> {
              IncidentStateMachine.requireAllowed(IncidentStatus.DETECTED, IncidentStatus.TRIAGED);
              IncidentStateMachine.requireAllowed(
                  IncidentStatus.TRIAGED, IncidentStatus.INVESTIGATING);
              IncidentStateMachine.requireAllowed(
                  IncidentStatus.INVESTIGATING, IncidentStatus.RESOLVED);
              IncidentStateMachine.requireAllowed(IncidentStatus.RESOLVED, IncidentStatus.CLOSED);
            })
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsSkippingFromDetectedToResolved() {
    assertThatThrownBy(
            () ->
                IncidentStateMachine.requireAllowed(
                    IncidentStatus.DETECTED, IncidentStatus.RESOLVED))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("DETECTED")
        .hasMessageContaining("RESOLVED");
  }
}
