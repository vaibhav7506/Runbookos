package com.vaibhav.runbookos.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the caller is authenticated but not permitted to perform the operation, including
 * attempts to reach another tenant's data. Deliberately does not distinguish "does not exist" from
 * "not permitted" at the message level, so it cannot be used to enumerate resources.
 */
public class AccessDeniedDomainException extends DomainException {

  public AccessDeniedDomainException(String message) {
    super("ACCESS_DENIED", message, HttpStatus.FORBIDDEN);
  }

  public static AccessDeniedDomainException insufficientRole() {
    return new AccessDeniedDomainException("Your role does not permit this operation");
  }

  public static AccessDeniedDomainException notAMember() {
    return new AccessDeniedDomainException("You are not a member of this organization");
  }
}
