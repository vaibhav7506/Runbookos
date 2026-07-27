# RunbookOS threat model

## Assets and trust boundaries

The Java control plane is the authorization boundary. Browser requests, inbound webhooks, n8n
callbacks, provider responses, logs, commits, tickets, model output, and model-supplied citations are
untrusted. PostgreSQL is authoritative state; Redis is disposable coordination state; n8n may
orchestrate only actions represented by a short-lived signed execution token.

Secrets cross only the setup boundary into AES-256-GCM encrypted credential references. APIs return
fingerprints, never plaintext. Disconnect overwrites ciphertext and revokes every usable reference.

## Principal threats and controls

| Threat | Control |
| --- | --- |
| Cross-tenant object access | Every repository read used by a controller includes organization ID; authoritative membership is rechecked. |
| Forged webhook or workflow callback | HMAC over timestamp, nonce, and exact body; replay nonce persistence; bounded timestamp window. |
| SSRF and redirect pivoting | HTTPS-only targets; DNS resolution validation; loopback, link-local, private, multicast, and metadata ranges denied unless an exact hostname is explicitly allowlisted; every redirect is revalidated. |
| Credential disclosure | Encrypted references, one-way DTOs, redacted audit metadata, no response-body logging, overwrite on revoke. |
| Prompt injection | Evidence is recursively sanitized, embedded instructions are rejected, evidence IDs are allowlisted, and structured model output is schema-validated. AI remains advisory. |
| Retry duplication | Idempotency keys, unique delivery IDs, transactional outbox IDs, and terminal state machines. |
| Outbox poison event | Bounded attempts, exponential backoff, dead-letter state, authorized audited redrive. |
| Audit tampering | Append-only database triggers plus a per-tenant SHA-256 hash chain. |
| Browser attacks | Restricted CORS, bearer-token stateless API, SameSite refresh cookie, CSP/frame denial, HSTS, nosniff, no-referrer, and permissions policy. |
| Resource exhaustion | 1 MiB request limit, auth/webhook rate limits, bounded outbox batches, semaphore bulkhead, database pagination. |

CSRF is disabled for the stateless bearer API. The refresh cookie is HttpOnly, Secure in production,
and SameSite; its response cannot be read by an untrusted origin because credentialed CORS is
restricted to configured origins.

Residual risks include compromised organization administrators, malicious credentials with broader
provider permissions than requested, and operators explicitly allowlisting private HTTP targets.
These are surfaced through audit events and should be governed operationally.
