package com.vaibhav.runbookos.config;

import static org.assertj.core.api.Assertions.*;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ResilienceConfigTest {
  @Test
  void circuitBreakerOpensAfterTheConfiguredFailureWindow() {
    CircuitBreaker breaker =
        new ResilienceConfig().circuitBreakerRegistry().circuitBreaker("provider-test");
    AtomicInteger attempts = new AtomicInteger();
    for (int index = 0; index < 5; index++) {
      assertThatThrownBy(
              () ->
                  breaker.executeRunnable(
                      () -> {
                        attempts.incrementAndGet();
                        throw new IllegalStateException("provider unavailable");
                      }))
          .isInstanceOf(IllegalStateException.class);
    }
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThatThrownBy(() -> breaker.executeRunnable(attempts::incrementAndGet))
        .isInstanceOf(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class);
    assertThat(attempts).hasValue(5);
  }
}
