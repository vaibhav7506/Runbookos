package com.vaibhav.runbookos.audit;

/** What kind of principal caused an audited event. */
public enum ActorType {
  /** A signed-in human. */
  USER,
  /** A scheduled job or internal process with no human initiator. */
  SYSTEM,
  /** An n8n workflow acting under a signed execution token. */
  WORKFLOW,
  /** An inbound external system, such as a verified webhook sender. */
  INTEGRATION,
  /** An unauthenticated caller, recorded for failed sign-in attempts. */
  ANONYMOUS
}
