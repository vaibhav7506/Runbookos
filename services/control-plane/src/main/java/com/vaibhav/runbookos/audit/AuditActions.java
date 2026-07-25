package com.vaibhav.runbookos.audit;

/**
 * Canonical audit action names. Kept as constants rather than free strings so that queries,
 * dashboards, and alerts can rely on stable identifiers, and so a typo cannot silently create a new
 * action that nobody reports on.
 *
 * <p>Naming convention: {@code RESOURCE_VERB}, past tense for completed facts.
 */
public final class AuditActions {

  // Authentication
  public static final String USER_SIGNED_UP = "USER_SIGNED_UP";
  public static final String USER_LOGIN_SUCCEEDED = "USER_LOGIN_SUCCEEDED";
  public static final String USER_LOGIN_FAILED = "USER_LOGIN_FAILED";
  public static final String USER_LOGGED_OUT = "USER_LOGGED_OUT";
  public static final String SESSION_REFRESHED = "SESSION_REFRESHED";
  public static final String SESSION_REUSE_DETECTED = "SESSION_REUSE_DETECTED";

  // Organizations and membership
  public static final String ORGANIZATION_CREATED = "ORGANIZATION_CREATED";
  public static final String ORGANIZATION_UPDATED = "ORGANIZATION_UPDATED";
  public static final String ORGANIZATION_SWITCHED = "ORGANIZATION_SWITCHED";
  public static final String MEMBERSHIP_ROLE_CHANGED = "MEMBERSHIP_ROLE_CHANGED";
  public static final String MEMBERSHIP_REMOVED = "MEMBERSHIP_REMOVED";

  // Integrations and secrets
  public static final String INTEGRATION_CREATED = "INTEGRATION_CREATED";
  public static final String INTEGRATION_UPDATED = "INTEGRATION_UPDATED";
  public static final String INTEGRATION_DISCONNECTED = "INTEGRATION_DISCONNECTED";
  public static final String CREDENTIAL_STORED = "CREDENTIAL_STORED";
  public static final String CREDENTIAL_ROTATED = "CREDENTIAL_ROTATED";
  public static final String CREDENTIAL_REVOKED = "CREDENTIAL_REVOKED";

  // Authorization
  public static final String ACCESS_DENIED = "ACCESS_DENIED";

  private AuditActions() {
    // Constants holder.
  }
}
