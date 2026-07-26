package com.vaibhav.runbookos.common.controller;

import com.vaibhav.runbookos.common.dto.HealthResponse;
import com.vaibhav.runbookos.common.dto.HealthResponse.ComponentHealth;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/** Aggregated health check endpoint that verifies all system dependencies. */
@RestController
@RequestMapping("/api/health")
public class HealthController {

  private final DataSource dataSource;
  private final RedisConnectionFactory redisConnectionFactory;
  private final RestClient restClient;
  private final String n8nBaseUrl;
  private final Instant startTime;

  public HealthController(
      DataSource dataSource,
      RedisConnectionFactory redisConnectionFactory,
      @Value("${runbookos.n8n.base-url:http://localhost:5678}") String n8nBaseUrl) {
    this.dataSource = dataSource;
    this.redisConnectionFactory = redisConnectionFactory;
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(2));
    requestFactory.setReadTimeout(Duration.ofSeconds(2));
    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    this.n8nBaseUrl = n8nBaseUrl;
    this.startTime = Instant.now();
  }

  @GetMapping
  public ResponseEntity<HealthResponse> health() {
    Map<String, ComponentHealth> components = new LinkedHashMap<>();

    components.put("postgresql", checkPostgres());
    components.put("redis", checkRedis());
    components.put("n8n", checkN8n());

    boolean allHealthy = components.values().stream().allMatch(c -> "UP".equals(c.status()));

    String overallStatus = allHealthy ? "UP" : "DEGRADED";

    long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
    String uptime = formatDuration(Duration.ofMillis(uptimeMs));

    HealthResponse response =
        new HealthResponse(overallStatus, Instant.now().toString(), uptime, components);

    int statusCode = allHealthy ? 200 : 503;
    return ResponseEntity.status(statusCode).body(response);
  }

  private ComponentHealth checkPostgres() {
    try (var connection = dataSource.getConnection()) {
      try (var statement = connection.createStatement()) {
        statement.execute("SELECT 1");
      }
      return new ComponentHealth("UP", "Connected", null);
    } catch (Exception e) {
      return new ComponentHealth("DOWN", "Connection failed", e.getMessage());
    }
  }

  private ComponentHealth checkRedis() {
    try {
      var connection = redisConnectionFactory.getConnection();
      String pong = connection.ping();
      connection.close();
      return new ComponentHealth("UP", "Connected (PONG: " + pong + ")", null);
    } catch (Exception e) {
      return new ComponentHealth("DOWN", "Connection failed", e.getMessage());
    }
  }

  private ComponentHealth checkN8n() {
    try {
      restClient.get().uri(n8nBaseUrl + "/healthz").retrieve().toBodilessEntity();
      return new ComponentHealth("UP", "Reachable", null);
    } catch (Exception e) {
      return new ComponentHealth("DOWN", "Unreachable", e.getMessage());
    }
  }

  private String formatDuration(Duration duration) {
    long hours = duration.toHours();
    long minutes = duration.toMinutesPart();
    long seconds = duration.toSecondsPart();
    return String.format("%dh %dm %ds", hours, minutes, seconds);
  }
}
