package com.vaibhav.runbookos.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Global exception handler that returns consistent structured error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<ErrorResponse.ValidationError> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fe ->
                    new ErrorResponse.ValidationError(
                        fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
            .toList();

    ErrorResponse response =
        new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            HttpStatus.BAD_REQUEST.value(),
            getCorrelationId(),
            errors);

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    List<ErrorResponse.ValidationError> errors =
        ex.getConstraintViolations().stream()
            .map(
                cv ->
                    new ErrorResponse.ValidationError(
                        cv.getPropertyPath().toString(), cv.getMessage(), cv.getInvalidValue()))
            .toList();

    ErrorResponse response =
        new ErrorResponse(
            "VALIDATION_ERROR",
            "Constraint violation",
            HttpStatus.BAD_REQUEST.value(),
            getCorrelationId(),
            errors);

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
    return buildResponse(
        "INVALID_REQUEST_BODY", "Request body is missing or malformed", HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex) {
    return buildResponse(
        "METHOD_NOT_ALLOWED", "HTTP method not allowed", HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex) {
    return buildResponse(
        "UNSUPPORTED_MEDIA_TYPE", "Content type not supported", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
    return buildResponse("NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    log.error("Unexpected error [correlationId={}]", getCorrelationId(), ex);
    return buildResponse(
        "INTERNAL_ERROR", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ErrorResponse> buildResponse(
      String code, String message, HttpStatus status) {
    ErrorResponse response = new ErrorResponse(code, message, status.value(), getCorrelationId());
    return ResponseEntity.status(status).body(response);
  }

  private String getCorrelationId() {
    String id = MDC.get("correlationId");
    return id != null ? id : "unknown";
  }
}
