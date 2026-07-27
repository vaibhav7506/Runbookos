package com.vaibhav.runbookos.workflow;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxEventTest {
  @Test
  void deadLettersAfterBoundedAttemptsAndCanBeExplicitlyRedriven() {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    OutboxEvent event =
        OutboxEvent.create(
            UUID.randomUUID(), "execution", UUID.randomUUID(), "START", Map.of(), now);

    for (int attempt = 0; attempt < 5; attempt++) {
      event.claim(now);
      event.failed("provider unavailable", now);
    }

    assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
    assertThat(event.getLastError()).isEqualTo("provider unavailable");
    event.redrive(now.plusSeconds(60));
    assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(event.getAttempts()).isZero();
    assertThat(event.getLastError()).isNull();
  }

  @Test
  void refusesToRedriveANonDeadEvent() {
    OutboxEvent event =
        OutboxEvent.create(
            UUID.randomUUID(), "execution", UUID.randomUUID(), "START", Map.of(), Instant.EPOCH);

    assertThatThrownBy(() -> event.redrive(Instant.EPOCH))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Only dead-letter events can be redriven");
  }
}
