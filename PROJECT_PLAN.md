# RunbookOS — Project Plan

## Current Phase: 4 — n8n Orchestration and Execution Engine

## Phase Status

| Phase | Name | Status |
|:--|:--|:--|
| 1 | Foundation, Architecture, and Design System | ✅ Complete |
| 2 | Identity, Multi-Tenancy, Security, and Data Model | ✅ Complete |
| 3 | Incident Ingestion, Deduplication, and Triage | ✅ Complete |
| 4 | n8n Orchestration and Execution Engine | ✅ Complete |
| 5 | Evidence-Grounded AI Incident Analysis | ⬜ Next |
| 6 | Runbooks, Policy Engine, and Human Approval | ⬜ Not started |
| 7 | Integrations and Complete Incident Workflow | ⬜ Not started |
| 8 | Reliability, Security, and Observability | ⬜ Not started |
| 9 | Product Polish and Complete UX | ⬜ Not started |
| 10 | Testing, Deployment, Documentation, and Final Audit | ⬜ Not started |

## Phase 2 — Completed

- Flyway identity, tenant, audit, integration, and encrypted credential schema
- BCrypt password hashing, short-lived JWT access tokens, and rotating opaque refresh sessions
- Signup, login, refresh, logout, current-user, organization creation, and switching APIs
- Backend-authoritative roles and organization membership checks
- Immutable database-enforced audit events
- AES-GCM credential storage that never returns plaintext
- Correlation IDs and structured global errors
- Springdoc OpenAPI and reproducible generated TypeScript contracts
- Registration, onboarding, setup choice, organization switching, and session-expiry UI
- Testcontainers clean-migration coverage and tenant, role, token, health, PostgreSQL, and Redis tests

## Phase 3 — Completed

- Incident, signal, evidence, assignment, comment, idempotency, and webhook-delivery models
- Validated incident state machine with audited transitions
- Sentry, GitHub deployment, and custom signed webhook routes
- Timestamp tolerance, nonce replay protection, delivery idempotency, and HMAC verification
- Size-limited, validated, recursively redacted payload storage
- Fingerprinting, delivery deduplication, and recent-incident grouping
- Severity, priority, service ownership, assignments, comments, search, filtering, and cursor pagination
- Responsive incident inbox/table/cards and incident detail workspace
- Deterministic Demo Incident Launcher

## Phase 4 — Completed

- Signed Spring Boot ↔ n8n protocol
- Short-lived execution tokens scoped to organization, execution, nonce, and allowed actions
- Execution, step, and immutable execution-event persistence with validated transitions
- Transactional outbox dispatch, bounded batches, exponential retry, and dead-letter state
- Versioned workflow registry and 11 validated n8n workflow exports
- Authenticated, replay-protected callbacks with backend action allowlisting
- Dispatch/callback idempotency, timeouts, manual execution retry, and failed-step retry
- Persisted execution timeline with SSE snapshot/live updates
- Demo workflow reports started, per-step simulated success, and completion

## Architectural Decisions

| ID | Decision | Rationale |
|:--|:--|:--|
| ADR-001 | Spring Boot 4.1.0 with Java 21 | Jakarta EE 11 and Java 21 baseline |
| ADR-002 | Gradle Kotlin DSL | Type-safe build configuration |
| ADR-003 | Next.js 16 App Router | Current server/client component model |
| ADR-004 | PostgreSQL is the source of truth | Tenant, execution, outbox, and audit durability |
| ADR-005 | Rotating refresh cookie plus short JWT | Limits exposure and detects replay |
| ADR-006 | Tenant ID in every organization-owned query | Prevents cross-tenant object access |
| ADR-007 | HMAC n8n protocol with action-scoped tokens | n8n orchestrates but never authorizes |
| ADR-008 | Transactional workflow outbox | State and dispatch intent commit atomically |
| ADR-009 | SSE with persisted snapshots | Live progress survives refresh |

## Unresolved Risks

| Risk | Severity | Mitigation |
|:--|:--|:--|
| Workflow exports require import and activation in local n8n | Low | Inactive-by-design exports are repository-validated |
| Demo workflow uses simulated providers | Expected | Real adapters are Phase 7 |
| OpenAPI generator audit includes transitive tooling advisories | Medium | Generator is development-only; track compatible upgrades |

## Test Status

| Gate | Status |
|:--|:--|
| Backend Spotless | ✅ Pass |
| Backend compile | ✅ Pass |
| Backend unit and integration tests | ✅ 19/19 pass |
| Clean Flyway migration | ✅ PostgreSQL 16 Testcontainers |
| Frontend format, lint, and strict type check | ✅ Pass |
| Frontend production build | ✅ Pass |
| Generated API contract | ✅ Pass |
| n8n workflow validation | ✅ Pass |

## Next Phase

Phase 5 — Evidence-Grounded AI Incident Analysis
