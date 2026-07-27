package com.vaibhav.runbookos.config;

import io.github.resilience4j.bulkhead.*;
import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.retry.*;
import java.time.Duration;
import org.springframework.context.annotation.*;
import org.springframework.web.client.RestClient;

@Configuration
public class ResilienceConfig {
  @Bean
  RestClient.Builder restClientBuilder() {
    return RestClient.builder();
  }

  @Bean
  CircuitBreakerRegistry circuitBreakerRegistry() {
    CircuitBreakerConfig config =
        CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .minimumNumberOfCalls(5)
            .slidingWindowSize(10)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(2)
            .build();
    return CircuitBreakerRegistry.of(config);
  }

  @Bean
  RetryRegistry retryRegistry() {
    return RetryRegistry.of(
        RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(250))
            .retryExceptions(org.springframework.web.client.ResourceAccessException.class)
            .build());
  }

  @Bean
  BulkheadRegistry bulkheadRegistry() {
    return BulkheadRegistry.of(
        BulkheadConfig.custom().maxConcurrentCalls(8).maxWaitDuration(Duration.ZERO).build());
  }

  @Bean
  TaggedCircuitBreakerMetrics circuitBreakerMetrics(CircuitBreakerRegistry registry) {
    return TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry);
  }
}
