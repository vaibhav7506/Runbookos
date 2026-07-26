package com.vaibhav.runbookos.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiProviderFailureTest {
  @Test
  void opensAfterThreeFailuresAndResetsAfterSuccess() {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    AiProviderFailure failure = AiProviderFailure.create(UUID.randomUUID(), AiProvider.OPENAI, now);
    failure.failed("timeout", now);
    failure.failed("timeout", now);
    assertThat(failure.isOpen(now)).isFalse();
    failure.failed("timeout", now);
    assertThat(failure.isOpen(now.plusSeconds(1))).isTrue();
    failure.succeeded(now.plusSeconds(2));
    assertThat(failure.isOpen(now.plusSeconds(2))).isFalse();
  }
}
