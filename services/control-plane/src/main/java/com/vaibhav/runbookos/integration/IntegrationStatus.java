package com.vaibhav.runbookos.integration;

/** Connection health of an integration. */
public enum IntegrationStatus {
  /** Created but not yet validated against the external system. */
  PENDING,
  /** Last validation or use succeeded. */
  CONNECTED,
  /** Working, but with reduced capability such as a missing optional permission. */
  DEGRADED,
  /** Deliberately disconnected by an operator; credentials are revoked. */
  DISCONNECTED,
  /** Last attempt failed. */
  ERROR
}
