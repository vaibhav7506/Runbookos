# RunbookOS incident recovery

1. Confirm impact from `/api/health`, container health, structured logs, Prometheus, and the operational health screen.
2. Preserve correlation IDs and avoid deleting failed outbox records or audit events.
3. If PostgreSQL is unavailable, stop write traffic and restore or fail over before restarting application instances.
4. If Redis is unavailable, restore Redis; authoritative incident and execution state remains in PostgreSQL.
5. If n8n is unavailable, leave outbox events pending. Circuit breakers and bounded retries prevent unbounded pressure. Restore n8n, validate its encryption key, then redrive only reviewed dead letters.
6. If an AI provider fails, Demo Mode or the configured fallback chain remains available; AI output is advisory and does not block manual incident work.
7. If credentials may be exposed, rotate provider credentials, HMAC/JWT keys, revoke refresh sessions, and record the response in the audit trail.
8. After recovery, reconcile executions, approvals, dead letters, webhook delivery IDs, and audit hashes before declaring service restored.

Never bypass the Java policy engine or directly mutate production incident/execution states to accelerate recovery.
