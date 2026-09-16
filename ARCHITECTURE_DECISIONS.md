# RunbookOS architecture decisions: evidence and inference

Reviewed against the working tree and `git log --all --oneline` / relevant `git log -p` history on 2026-09-16. **VERIFIED** means the repository explicitly states a reason; **INFERRED** means the pattern suggests a reason but no source states it; **UNKNOWN** means a rationale cannot be established. This is an evidence inventory, not a retrospective claim that all intended controls work.

## Java/Spring Boot as the control plane — VERIFIED in part

`README.md` (“Why Java and n8n are separate”) and `docs/architecture/workflow-boundaries.md` explicitly place identity, tenant access, policy, incident/execution state, approvals, persistence, and audit in Spring Boot: “Spring Boot is the control plane; n8n is an orchestration worker. This is a security boundary.” `PROJECT_PLAN.md` ADR-001 says Spring Boot 4.1/Java 21 supplies the Jakarta EE 11 and Java 21 baseline. The specific reason Spring Boot was preferred over another backend framework is **UNKNOWN**; git history labels phases but adds no comparative rationale.

## n8n for bounded orchestration — VERIFIED boundary; UNKNOWN product comparison

`README.md` and `docs/architecture/workflow-boundaries.md` explicitly separate orchestration from Java authorization so workflow configuration cannot become the policy authority. `automation/n8n/workflows/*.json` shows composable exports and webhook/sub-workflow nodes. Why n8n was chosen instead of another orchestrator or implementing the same bounded steps in Java is **UNKNOWN**; no comparison or cost/maintainability rationale appears in the reviewed docs or commit messages. All exports are currently inactive and require import/activation, so the intended boundary is not proof of live orchestration.

## Short JWT plus rotating refresh sessions — VERIFIED

`PROJECT_PLAN.md` ADR-005 says the combination “limits exposure and detects replay.” `docs/security/secret-handling.md` notes refresh tokens are stored as hashes and access tokens are short lived. `JwtService.issue()`/`verify()` in `services/control-plane/src/main/java/com/vaibhav/runbookos/security/JwtService.java` use HS256 access JWTs; `AuthService.issue()`/`refresh()` use opaque hashed tokens, rotation, and revoked-token reuse detection. Why this was preferred over server-side sessions specifically is **INFERRED** (stateless bearer verification and browser refresh-cookie renewal); no explicit sessions comparison was found in docs, comments, or the phase-two commit.

## AES-256-GCM for stored credentials — VERIFIED

`SecretCipher` Javadoc in `services/control-plane/src/main/java/com/vaibhav/runbookos/security/crypto/SecretCipher.java` explicitly says GCM was chosen over CBC so ciphertext tampering is detected rather than yielding silent garbage. It requires a Base64 key decoding to exactly 32 bytes, generates a fresh random 96-bit nonce, uses a 128-bit authentication tag, and stores key ID alongside ciphertext for rotation. `docs/security/secret-handling.md` requires separate backup of n8n's own encryption key. Commit `c27d54c` introduced this rationale in the original Javadoc.

## HMAC-signed Java↔n8n messages — VERIFIED for boundary; INFERRED versus mTLS

`docs/security/threat-model.md` names forged webhooks/callbacks as a threat and specifies HMAC over timestamp, nonce, and exact body plus nonce persistence and timestamp tolerance. `docs/architecture/workflow-boundaries.md` says n8n may only carry already-authorized scoped actions and callbacks must be revalidated by Java. `HmacSigner` and `WorkflowCallbackService.accept()` implement the Java side; `automation/n8n/workflows/runbookos-execute.v1.json` contains the n8n signature check. Why HMAC was selected **instead of mutual TLS** is **INFERRED** (application-level authenticated payloads and action scope across ordinary HTTP/private networking); no explicit mTLS comparison was found. Private n8n networking is the human's 2026-09-16 deployment decision, not a replacement for HMAC.

## Transactional outbox — VERIFIED intent and operations

`PROJECT_PLAN.md` ADR-008 states “State and dispatch intent commit atomically.” `ExecutionService.create()` writes an execution, steps, and `WORKFLOW_DISPATCH` outbox event together; `ExecutionService.retry()` creates another `WORKFLOW_DISPATCH`; `retryStep()` creates `STEP_RETRY`; `ApprovalService.authorize()` writes `APPROVAL_REQUESTED` when Java policy requires human approval. `OutboxProcessor.dispatch()`/`dispatchApproval()` deliver execution and approval events to two n8n webhooks. These are the specific operations; incident ingest and AI analysis do **not** use this outbox. **Current implementation gap:** failure catch in `OutboxProcessor.process()` does not call `OutboxEvent.failed()`, leaving claimed events in `PROCESSING`; see `WORKFLOW.md`. `git log -p` shows that call was removed in commit `cc133cd`, without an explanatory rationale.

## Retries, circuit breakers, and bulkheads — VERIFIED locations; INFERRED tuning

`OutboxProcessor.resilientDispatch()` decorates only n8n webhook dispatch (`n8nDispatch`) with bulkhead, circuit breaker, and retry. `ResilienceConfig` sets eight concurrent calls, a 50% breaker threshold after at least five calls, and up to three attempts for `ResourceAccessException` with 250 ms waits. `AiAnalysisService.analyze()` separately uses config-specific provider retries and `AiProviderFailure` opens a per-provider 60-second circuit after three failures; `RemoteAiModelClient` applies provider connect/read timeouts. `docs/operations/incident-recovery.md` explicitly says these controls should prevent unbounded pressure when n8n is down and allow AI fallback when a provider fails. The numerical thresholds and ordering of the decorators are **INFERRED** engineering choices; no tuning evidence or production baseline was found. Outbox stuck-state means the documented recovery behavior does not currently follow from these controls.

## Hash-chained audit trail — VERIFIED intent; implementation caveat

`docs/security/threat-model.md` states append-only triggers plus a per-tenant SHA-256 chain defend against audit tampering. `AuditEvent` Javadoc in `services/control-plane/src/main/java/com/vaibhav/runbookos/audit/AuditEvent.java` explains no setters plus database update/delete rejection. `AuditService` Javadoc explains `REQUIRES_NEW` so denial records survive caller rollback. `chain_audit_event()` in `services/control-plane/src/main/resources/db/migration/V8__reliability_security_observability.sql` links a new row to the latest hash in the same organization and hashes defined row fields. A plain append-only log would not have this tamper-evidence property; that comparison is **INFERRED** from the stated threat. No verification routine was found, old rows are backfilled as independent zero-root hashes, and concurrent insert serialization is not established.

## AI provider fallback order — VERIFIED mechanism; UNKNOWN business rationale

`AiAnalysisService.analyze()` calls `AiProviderConfigRepository.findByOrganizationIdAndEnabledTrueOrderByPriorityAsc()`, then attempts providers in ascending configured priority until one succeeds; the enum and `RemoteAiModelClient` support OpenAI, Anthropic, Gemini, Groq, and a compatible HTTPS endpoint. There is **no fixed provider order** in the implementation. `README.md` says admins select a provider and that fallback is controlled, but neither README, code comments, nor the phase-six commit (`46c8ff2`, “Adding phase 6 codes”) states a preferred ordering by price, speed, or capability. The reason for any administrator-assigned priority is **UNKNOWN**. The deterministic client is used after all live choices fail or when none is enabled.

## Demo Mode — VERIFIED behavior and purpose in part

`README.md` calls Demo Mode credential-free, no-cost, non-destructive, and suitable for guided evaluation. `AiAnalysisService.analyze()` skips remote providers for a demo organization and calls `DemoAiModelClient`, which returns a fixed advisory structured output with evidence IDs or hypotheses. `IncidentController.demo()` seeds a checkout incident, deployment/metric evidence, and demo runbooks. `PolicyEngine.evaluate()` denies HIGH_RISK in Demo Mode and PROHIBITED everywhere. The n8n execution export reports simulated per-step completion. These support local testing/demos and avoiding paid provider use (**VERIFIED** from README); any sales-demo motivation is **UNKNOWN**. A non-demo organization also falls back to deterministic output when no external provider succeeds, so “Demo Mode” is not the only route to the deterministic analyzer.

## PostgreSQL and tenant-scoped authorization — VERIFIED

`PROJECT_PLAN.md` ADR-004 calls PostgreSQL the durable source of truth for tenant, execution, outbox, and audit state. `TenantAccessService` Javadoc explicitly calls membership authoritative and JWT role claims hints; its `require()` reloads active organization membership and role before service actions. `docs/security/threat-model.md` identifies cross-tenant object access as a principal threat. The use of organization-scoped repository queries is observable in `IncidentService.require()`, `ExecutionService.require()`, and `ApprovalService.require()`.

## 2026-09-16 deployment-specific n8n exposure choice — VERIFIED human decision, not deployed

`DECISIONS.md` records the human decision not to add an authentication proxy or public n8n domain, using `n8n.railway.internal` instead, and to set `REFRESH_COOKIE_SECURE=true` for HTTPS. This is a deployment instruction; it is **not** implemented by local `docker-compose.yml`, which publishes n8n port 5678. The private-only plan also requires an operator method to import/activate inactive workflows; no such production procedure has been chosen in this session.

## Git-history scope

`git log --all --oneline` contains eight commits through `4c96164`. Relevant `git log -p` checks covered `AuthService`, `SecretCipher`, `OutboxProcessor`, and migration V8. Except for the initial `SecretCipher` Javadoc and a resilience comment about preserving retryable exception types, commit subjects are phase/addition/fix labels, not architectural trade-off records. Claims marked INFERRED or UNKNOWN above were not promoted to VERIFIED merely because a feature appeared in a commit.
