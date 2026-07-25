package com.vaibhav.runbookos.common.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaibhav.runbookos.common.dto.HealthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Integration test for the /api/health endpoint. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class HealthControllerTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("runbookos_test")
          .withUsername("test")
          .withPassword("test");

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
  }

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void healthEndpointReturnsStructuredResponse() {
    ResponseEntity<HealthResponse> response =
        restTemplate.getForEntity("/api/health", HealthResponse.class);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isNotNull();
    assertThat(response.getBody().timestamp()).isNotNull();
    assertThat(response.getBody().uptime()).isNotNull();
    assertThat(response.getBody().components()).isNotNull();
  }

  @Test
  void healthEndpointIncludesAllComponents() {
    ResponseEntity<HealthResponse> response =
        restTemplate.getForEntity("/api/health", HealthResponse.class);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().components()).containsKeys("postgresql", "redis", "n8n");
  }

  @Test
  void healthEndpointReturnsCorrelationIdHeader() {
    ResponseEntity<HealthResponse> response =
        restTemplate.getForEntity("/api/health", HealthResponse.class);

    assertThat(response.getHeaders().getFirst("X-Correlation-ID")).isNotBlank();
  }

  @Test
  void healthEndpointShowsPostgresUp() {
    ResponseEntity<HealthResponse> response =
        restTemplate.getForEntity("/api/health", HealthResponse.class);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().components().get("postgresql").status()).isEqualTo("UP");
  }

  @Test
  void healthEndpointShowsRedisUp() {
    ResponseEntity<HealthResponse> response =
        restTemplate.getForEntity("/api/health", HealthResponse.class);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().components().get("redis").status()).isEqualTo("UP");
  }

  @Test
  void healthEndpointReturnsOkOrServiceUnavailable() {
    ResponseEntity<HealthResponse> response =
        restTemplate.getForEntity("/api/health", HealthResponse.class);

    assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.SERVICE_UNAVAILABLE);
  }
}
