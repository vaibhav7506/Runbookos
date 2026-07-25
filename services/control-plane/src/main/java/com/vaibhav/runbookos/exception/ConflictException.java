package com.vaibhav.runbookos.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a request conflicts with existing state, such as a duplicate email or slug. */
public class ConflictException extends DomainException {

  public ConflictException(String code, String message) {
    super(code, message, HttpStatus.CONFLICT);
  }
}
