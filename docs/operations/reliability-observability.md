# Reliability and observability

Workflow dispatch uses a transactional outbox. A dispatcher claims at most 20 ready events, applies
a semaphore bulkhead, circuit breaker, bounded retry, and the configured HTTP timeout, then marks the
same dispatch ID delivered. Five exhausted deliveries become dead letters. Owners and administrators
can inspect and redrive them from the operational health surface; redrive resets attempts but retains
the stable event ID so downstream processing remains idempotent.

The application shuts down gracefully with a 30-second phase timeout. Active incidents and
executions have partial indexes. List endpoints are paged or explicitly bounded. Operational
retention should archive resolved incidents and terminal executions after 90 days, retain audit
events for at least 365 days, and purge expired nonce/idempotency rows after 24 hours. Archival columns
are present; an installation may export records before setting `archived_at`.

## Metrics

The protected `/actuator/prometheus` endpoint exports:

- `runbookos_incidents_created_total`, `runbookos_incidents_deduplicated_total`, and
  `runbookos_incidents_active`
- `runbookos_workflows_duration_seconds` and `runbookos_workflows_failures_total`
- `runbookos_providers_latency_seconds` and `runbookos_providers_errors_total`
- `runbookos_ai_tokens_total{direction=...}` and `runbookos_ai_estimated_cost_usd_total`
- tagged Resilience4j circuit-breaker metrics
- standard JVM, HTTP server, datasource, and process metrics

Every request receives `X-Correlation-ID`; logs include correlation and Micrometer trace identifiers.
OTLP export is configured with `OTEL_EXPORTER_OTLP_ENDPOINT`. n8n dispatches carry a stable dispatch
ID and signed callback metadata, connecting web request, control plane, workflow, callback, and
provider activity.

Do not expose Actuator metrics through a public reverse proxy. Prometheus should reach the control
plane on a private network and authenticate at the network or proxy boundary.
