package com.vaibhav.runbookos.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested resource does not exist, or is not visible to the current tenant. */
public class ResourceNotFoundException extends DomainException {

  public ResourceNotFoundException(String resourceType, Object identifier) {
    super(
        "RESOURCE_NOT_FOUND",
        "%s not found: %s".formatted(resourceType, identifier),
        HttpStatus.NOT_FOUND);
  }
}
