package com.vaibhav.runbookos.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Indirection over the system clock so time-dependent behaviour (token expiry, approval expiry,
 * session rotation) is deterministically testable. Production code must never call {@code
 * Instant.now()} directly.
 */
public interface TimeProvider {

  Instant now();

  /**
   * Postgres {@code TIMESTAMPTZ} preserves microsecond precision while {@link Instant} carries
   * nanoseconds. Truncating on write keeps in-memory values equal to values read back, which
   * otherwise breaks equality assertions in tests.
   */
  default Instant nowTruncated() {
    return now().truncatedTo(ChronoUnit.MICROS);
  }

  @Configuration
  class TimeProviderConfiguration {
    @Bean
    public TimeProvider systemTimeProvider() {
      return Instant::now;
    }
  }
}
