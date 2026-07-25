package com.vaibhav.runbookos.audit;

/** Result of an audited operation. */
public enum AuditOutcome {
  /** The operation completed. */
  SUCCESS,
  /** The operation was permitted but failed, for example a downstream error. */
  FAILURE,
  /** The operation was refused by authentication, authorization, or policy. */
  DENIED
}
