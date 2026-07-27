-- Forward-only compatibility fix for Hibernate 7's String JDBC mapping.
ALTER TABLE integration_credential_references
    ALTER COLUMN fingerprint TYPE VARCHAR(12);

CREATE TABLE integration_usage_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    integration_id UUID NOT NULL REFERENCES integrations(id) ON DELETE CASCADE,
    operation VARCHAR(80) NOT NULL,
    outcome VARCHAR(24) NOT NULL,
    detail VARCHAR(512),
    correlation_id VARCHAR(128),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_integration_usage_outcome CHECK (outcome IN ('SUCCEEDED', 'DEGRADED', 'FAILED'))
);

CREATE INDEX idx_integration_usage_recent
    ON integration_usage_events (organization_id, integration_id, occurred_at DESC);

CREATE TABLE incident_postmortems (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    summary TEXT NOT NULL,
    impact TEXT NOT NULL,
    root_cause TEXT NOT NULL,
    resolution TEXT NOT NULL,
    follow_up_actions JSONB NOT NULL DEFAULT '[]'::jsonb,
    generated_by UUID NOT NULL REFERENCES users(id),
    generated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_incident_postmortem UNIQUE (incident_id)
);

CREATE INDEX idx_postmortem_org_generated
    ON incident_postmortems (organization_id, generated_at DESC);

CREATE INDEX idx_integration_health
    ON integrations (organization_id, status, updated_at DESC);
