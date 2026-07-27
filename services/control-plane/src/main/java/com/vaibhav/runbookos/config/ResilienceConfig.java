package com.vaibhav.runbookos.config;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
// import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
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
    RetryConfig config =
        RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(250))
            .retryExceptions(ResourceAccessException.class)
            .build();

    return RetryRegistry.of(config);
  }

  @Bean
  BulkheadRegistry bulkheadRegistry() {
    BulkheadConfig config =
        BulkheadConfig.custom().maxConcurrentCalls(8).maxWaitDuration(Duration.ZERO).build();

    return BulkheadRegistry.of(config);
  }

  @Bean
  TaggedCircuitBreakerMetrics circuitBreakerMetrics(CircuitBreakerRegistry registry) {
    return TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry);
  }
}
