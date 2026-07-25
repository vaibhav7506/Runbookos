package com.vaibhav.runbookos.exception;

import java.time.Instant;
import java.util.List;

/**
 * Structured error response returned by all API endpoints on failure.
 *
 * @param code machine-readable error code
 * @param message human-readable error message
 * @param status HTTP status code
 * @param correlationId request correlation ID for tracing
 * @param timestamp ISO-8601 timestamp
 * @param validationErrors field-level validation errors, if applicable
 */
public record ErrorResponse(
    String code,
    String message,
    int status,
    String correlationId,
    String timestamp,
    List<ValidationError> validationErrors) {

  public ErrorResponse(String code, String message, int status, String correlationId) {
    this(code, message, status, correlationId, Instant.now().toString(), List.of());
  }

  public ErrorResponse(
      String code,
      String message,
      int status,
      String correlationId,
      List<ValidationError> validationErrors) {
    this(code, message, status, correlationId, Instant.now().toString(), validationErrors);
  }

  /**
   * Individual field validation error.
   *
   * @param field the field that failed validation
   * @param message the validation error message
   * @param rejectedValue the rejected value, if available
   */
  public record ValidationError(String field, String message, Object rejectedValue) {}
}
