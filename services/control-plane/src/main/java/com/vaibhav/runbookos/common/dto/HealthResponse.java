package com.vaibhav.runbookos.common.dto;

import java.util.Map;

/**
 * Structured health response returned by the /api/health endpoint.
 *
 * @param status overall system status (UP, DEGRADED, DOWN)
 * @param timestamp ISO-8601 timestamp of the health check
 * @param uptime human-readable uptime string
 * @param components individual component health statuses
 */
public record HealthResponse(
    String status, String timestamp, String uptime, Map<String, ComponentHealth> components) {

  /**
   * Health status for an individual component.
   *
   * @param status UP or DOWN
   * @param message descriptive message
   * @param error error message if unhealthy, null otherwise
   */
  public record ComponentHealth(String status, String message, String error) {}
}
