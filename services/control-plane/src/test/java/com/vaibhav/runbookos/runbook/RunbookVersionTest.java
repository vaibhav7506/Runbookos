package com.vaibhav.runbookos.runbook;

import static org.assertj.core.api.Assertions.*;

import com.vaibhav.runbookos.exception.ConflictException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RunbookVersionTest {
  @Test
  void publishedVersionBecomesImmutable() {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    RunbookVersion version =
        RunbookVersion.draft(
            UUID.randomUUID(), UUID.randomUUID(), 1, "initial", UUID.randomUUID(), now);
    version.publish(UUID.randomUUID(), now);
    assertThatThrownBy(version::requireDraft)
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("cannot be modified");
  }
}
