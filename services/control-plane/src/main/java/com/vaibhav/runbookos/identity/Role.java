package com.vaibhav.runbookos.identity;

/**
 * Role held by a user within a single organization. Ordered from most to least privileged for
 * READ-side comparisons; write capabilities are expressed explicitly rather than inferred from
 * ordering, because AUDITOR is deliberately not a subset of RESPONDER.
 */
public enum Role {

  /** Full control, including billing, deletion, and transfer of ownership. */
  OWNER,

  /** Manages members, integrations, runbooks, and policies. */
  ADMIN,

  /** Works incidents: triage, comment, execute runbooks, approve reversible actions. */
  RESPONDER,

  /** Read-only access to incidents and runbooks. */
  VIEWER,

  /** Read-only access to the audit trail and approval records, for compliance review. */
  AUDITOR;

  /** Authority name as seen by Spring Security expressions, e.g. {@code hasAuthority('ROLE_ADMIN')}. */
  public String authority() {
    return "ROLE_" + name();
  }

  /** Whether this role may change organization membership or settings. */
  public boolean canAdministerOrganization() {
    return this == OWNER || this == ADMIN;
  }

  /**
   * Whether this role may approve a REVERSIBLE action. Elevated (HIGH_RISK) approval additionally
   * requires two distinct approvers and is decided by the Phase 6 policy engine, not here.
   */
  public boolean canApprove() {
    return this == OWNER || this == ADMIN || this == RESPONDER;
  }

  /** Whether this role may act on incidents, as opposed to only reading them. */
  public boolean canRespond() {
    return this == OWNER || this == ADMIN || this == RESPONDER;
  }

  /** Whether this role may read the immutable audit trail. */
  public boolean canReadAudit() {
    return this == OWNER || this == ADMIN || this == AUDITOR;
  }
}
