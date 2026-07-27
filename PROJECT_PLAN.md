# RunbookOS — Project Plan

## Current Phase: 9 — Product Polish and Complete UX

## Phase Status

| Phase | Name | Status |
|:--|:--|:--|
| 1 | Foundation, Architecture, and Design System | ✅ Complete |
| 2 | Identity, Multi-Tenancy, Security, and Data Model | ✅ Complete |
| 3 | Incident Ingestion, Deduplication, and Triage | ✅ Complete |
| 4 | n8n Orchestration and Execution Engine | ✅ Complete |
| 5 | Evidence-Grounded AI Incident Analysis | ✅ Complete |
| 6 | Runbooks, Policy Engine, and Human Approval | ✅ Complete |
| 7 | Integrations and Complete Incident Workflow | ✅ Complete |
| 8 | Reliability, Security, and Observability | ✅ Complete |
| 9 | Product Polish and Complete UX | ✅ Complete |
| 10 | Testing, Deployment, Documentation, and Final Audit | ⬜ Next |

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

## Phase 5 — Completed

- Evidence-grounded structured analysis with citation and hypothesis validation
- Recursive secret redaction and prompt-injection instruction rejection before provider calls
- Encrypted BYOK configuration for OpenAI, Anthropic, Gemini, Groq, and compatible endpoints
- Deterministic no-cost Demo Mode analyzer and controlled provider fallback
- Bounded provider timeouts, retries, persisted circuit state, token budgets, and cost ceilings
- Prompt template/version metadata without stored provider keys
- Provider, model, latency, token, cost, fallback, and quality telemetry
- Reproducible six-case incident evaluation dataset and validation script
- Clearly advisory incident analysis UI with linked evidence and usage details

## Phase 6 — Completed

- Versioned runbooks with editable drafts and immutable published versions
- Structured vertical steps covering all required types, risks, roles, retries, rollback, and environments
- Java policy engine with auditable reasons and protected safety invariants
- Development, staging, and production default policies
- Read-only allow, reversible approval, high-risk two-person approval, and prohibited deny behavior
- Approval expiry, incident-state cancellation, elevated self-approval prevention, and typed confirmation
- Backend-authoritative signed Slack/email workflow callbacks with nonce replay protection
- Runbook library, detail, step builder, policy preview/editor, and approval inbox/detail UI

## Phase 7 — Completed

- Organization-isolated GitHub, Sentry, Slack, email, Jira, signed webhook, and HTTP health-check integrations
- Encrypted credentials with redacted responses, explicit permission scopes, validation health, usage history, and credential revocation
- Live GitHub validation plus transparent no-credential demo adapters for every other connector
- Three realistic starter runbooks and a deterministic end-to-end demo incident
- Persisted, printable incident postmortems generated only after resolution

## Phase 8 — Completed

- Resilience4j retries, timeouts, circuit breakers, bulkheads, transactional outbox backpressure, dead-letter inspection, and audited redrive
- Strict request limits, content-type enforcement, HMAC/replay defenses, rate limits, security headers, and SSRF-safe outbound targets
- Integrity-chained audit records, archival/indexing migrations, pagination, and organization-isolated operational endpoints
- Structured correlation logging, OpenTelemetry tracing, Micrometer metrics, Prometheus configuration, and Grafana dashboard
- Threat model, reliability runbook, dependency scanning, container scanning, resilience tests, and SSRF tests

## Phase 9 — Completed

- Operational overview and health dashboards with incident, approval, execution, integration, AI-cost, and dead-letter signals
- Three-region incident workspace with a precise response timeline, evidence and analysis drawer, contextual actions, notes, and printable postmortems
- Command palette, keyboard shortcuts, recent destinations, saved incident filters, copyable deep links, and locale-aware exact times
- Guided first-run demo, polished landing preview, responsive mobile navigation, dark theme, loading/error/empty states, and reduced-motion support
- Unsaved-change protection, destructive confirmations, inline action feedback, and responsive approval, digest, and resolution email templates

## Phase 10 — Completed

- Expanded backend coverage for HMAC tamper rejection, optimistic approval locking, provider HTTP boundaries, outbox dead-letter/redrive behavior, policy invariants, authentication, state machines, tenant isolation, and SSRF defenses
- Added Vitest component, validation, and API-client tests plus Playwright desktop/mobile coverage for registration, onboarding, demo incident creation, grounded analysis, lifecycle transitions, postmortem generation, and WCAG checks
- Completed CI gates for formatting, compilation, backend/frontend tests, workflow exports, deterministic analysis evaluation, secret/dependency/container scanning, Docker image builds, and browser E2E
- Added non-root production images, health checks, graceful shutdown, staging/production profiles, queue-mode n8n worker deployment, internal networking, authenticated Redis, TLS reverse proxy, and optional observability profile
- Added deployment, backup/restore, incident-recovery, security, data-model, workflow-boundary, demo, and final-audit documentation
- Rebuilt the project README with architecture, quick start, exact VS Code commands, configuration, security, operations, testing, limitations, roadmap, and portfolio guidance
- Completed the final local audit with healthy containers, zero npm production advisories, valid workflow exports, deterministic analysis evaluation, and passing desktop/mobile browser flows

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
| ADR-010 | Model recommendations contain no executable payload | AI remains advisory and cannot bypass policy |
| ADR-011 | Published runbook versions are immutable | Execution intent remains reviewable and reproducible |
| ADR-012 | Java policy decisions precede every action authorization | Workflow tooling never becomes the authority |
| ADR-013 | Elevated approval requires two distinct eligible users | Prevents unilateral high-risk remediation |

## Unresolved Risks

| Risk | Severity | Mitigation |
|:--|:--|:--|
| Workflow exports require import and activation in local n8n | Low | Inactive-by-design exports are repository-validated |
| Non-GitHub external adapters use transparent simulations without credentials | Expected | Configure provider credentials when validating production deployments |
| External AI output quality varies by model | Medium | Deterministic schema, citation, budget, and quality gates reject unsafe output |
| Slack/email identity mapping depends on Phase 7 adapters | Low | Signed callbacks still re-check tenant membership and role in Spring Boot |

## Test Status

| Gate | Status |
|:--|:--|
| Backend Spotless | ✅ Pass |
| Backend compile | ✅ Pass |
| Backend unit and integration tests | ✅ 42/42 pass |
| Clean Flyway migration | ✅ PostgreSQL 16 Testcontainers |
| Frontend format, lint, and strict type check | ✅ Pass |
| Frontend component, validation, and API tests | ✅ 7/7 pass |
| Frontend production build | ✅ Pass |
| Desktop/mobile Playwright and WCAG flows | ✅ 4/4 pass |
| npm production dependency audit | ✅ 0 known vulnerabilities |
| Generated API contract | ✅ Pass |
| n8n workflow validation | ✅ 12 exports pass |
| Analysis evaluation dataset | ✅ 6/6 pass |
| Local Docker release stack | ✅ All services healthy |

## Project Status

All ten planned phases are complete. Future work is tracked as roadmap scope rather than unfinished
phase acceptance criteria.
