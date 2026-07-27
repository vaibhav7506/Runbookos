# Data model

RunbookOS stores authoritative control-plane state in PostgreSQL. Every organization-owned aggregate carries an `organization_id`; service and repository queries require that identifier.

```mermaid
erDiagram
  USER ||--o{ MEMBERSHIP : has
  ORGANIZATION ||--o{ MEMBERSHIP : contains
  ORGANIZATION ||--o{ INCIDENT : owns
  INCIDENT ||--o{ INCIDENT_SIGNAL : groups
  INCIDENT ||--o{ INCIDENT_EVIDENCE : supports
  INCIDENT ||--o{ INCIDENT_ANALYSIS : receives
  INCIDENT ||--o{ EXECUTION : starts
  RUNBOOK ||--o{ RUNBOOK_VERSION : versions
  RUNBOOK_VERSION ||--o{ RUNBOOK_STEP : contains
  EXECUTION ||--o{ STEP_EXECUTION : tracks
  EXECUTION ||--o{ EXECUTION_EVENT : records
  EXECUTION ||--o{ APPROVAL_REQUEST : gates
  APPROVAL_REQUEST ||--o{ APPROVAL_DECISION : receives
  ORGANIZATION ||--o{ INTEGRATION : configures
  ORGANIZATION ||--o{ AUDIT_EVENT : records
  INCIDENT ||--o| INCIDENT_POSTMORTEM : produces
```

UUIDs are used throughout and timestamps are UTC. Published runbook versions and audit events are immutable. Executions, incidents, approvals, integrations, and refresh sessions use optimistic or explicit state validation where concurrent decisions matter. Flyway owns schema evolution; released migrations are never edited.

Sensitive values are separated from normal integration metadata. Credential ciphertext uses AES-GCM, stores its nonce and key identifier, and is never returned through response DTOs.
