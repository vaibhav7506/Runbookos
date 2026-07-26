package com.vaibhav.runbookos.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for errors that are part of the domain contract. Each carries a stable
 * machine-readable code and an HTTP status, so {@link GlobalExceptionHandler} can translate it
 * without a growing chain of instanceof checks. Messages on these exceptions are safe to return to
 * clients; anything sensitive belongs in the log, not here.
 */
public abstract class DomainException extends RuntimeException {

  private final String code;
  private final HttpStatus status;

  protected DomainException(String code, String message, HttpStatus status) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public String getCode() {
    return code;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
