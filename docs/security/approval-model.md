# Approval model

The Java policy engine evaluates every runbook step before orchestration.

| Risk | Default decision | Required control |
|---|---|---|
| `READ_ONLY` | Allow | Valid tenant, role, state, and step |
| `REVERSIBLE` | Require approval | One eligible responder or administrator |
| `HIGH_RISK` | Elevated approval | Two distinct eligible users, exact confirmation phrase, reason; disabled in Demo Mode |
| `PROHIBITED` | Deny | Cannot be weakened by configuration |

Approval requests are bound to an organization, incident, execution, immutable runbook version, and step key. They expire, are cancelled when the incident state invalidates them, reject duplicate approvers, and prevent elevated self-approval. Slack and email decisions are signed callbacks, but Spring Boot still reloads membership and policy before accepting them.

The `@Version` field protects sensitive concurrent updates. Decisions, denial reasons, expiry, cancellation, and final authorization are written to the audit chain.
