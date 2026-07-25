package com.vaibhav.runbookos.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when credentials are missing, malformed, expired, or rejected. The message is intentionally
 * uniform across "unknown email" and "wrong password" so it cannot be used to enumerate accounts.
 */
public class AuthenticationDomainException extends DomainException {

  public AuthenticationDomainException(String code, String message) {
    super(code, message, HttpStatus.UNAUTHORIZED);
  }

  public static AuthenticationDomainException invalidCredentials() {
    return new AuthenticationDomainException(
        "INVALID_CREDENTIALS", "Email or password is incorrect");
  }

  public static AuthenticationDomainException invalidRefreshSession() {
    return new AuthenticationDomainException(
        "INVALID_REFRESH_SESSION", "Your session is no longer valid. Please sign in again.");
  }

  public static AuthenticationDomainException accountNotActive() {
    return new AuthenticationDomainException(
        "ACCOUNT_NOT_ACTIVE", "This account is not active. Contact an administrator.");
  }
}
