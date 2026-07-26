CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    title VARCHAR(240) NOT NULL,
    summary TEXT,
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(8) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 3,
    affected_service VARCHAR(120) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    assignee_user_id UUID,
    signal_count INTEGER NOT NULL DEFAULT 1,
    first_detected_at TIMESTAMPTZ NOT NULL,
    last_signal_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_incident_status CHECK (status IN ('DETECTED','TRIAGED','INVESTIGATING','AWAITING_APPROVAL','MITIGATING','MONITORING','RESOLVED','CLOSED','CANCELLED')),
    CONSTRAINT ck_incident_severity CHECK (severity IN ('SEV1','SEV2','SEV3','SEV4')),
    CONSTRAINT ck_incident_priority CHECK (priority BETWEEN 1 AND 4)
);
CREATE INDEX ix_incidents_org_status_time ON incidents(organization_id, status, last_signal_at DESC);
CREATE INDEX ix_incidents_org_fingerprint ON incidents(organization_id, fingerprint, last_signal_at DESC);
CREATE INDEX ix_incidents_org_service ON incidents(organization_id, affected_service);

CREATE TABLE incident_signals (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    source VARCHAR(32) NOT NULL,
    external_id VARCHAR(255),
    fingerprint VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_signal_source_external UNIQUE(organization_id, source, external_id)
);
CREATE INDEX ix_signals_incident_time ON incident_signals(incident_id, received_at);

CREATE TABLE incident_evidence (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    kind VARCHAR(48) NOT NULL,
    title VARCHAR(240) NOT NULL,
    content JSONB NOT NULL,
    source VARCHAR(120),
    collected_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_evidence_incident_time ON incident_evidence(incident_id, collected_at);

CREATE TABLE incident_assignments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    assignee_user_id UUID NOT NULL REFERENCES users(id),
    assigned_by UUID NOT NULL REFERENCES users(id),
    assigned_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE incident_comments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    author_user_id UUID NOT NULL REFERENCES users(id),
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_comments_incident_time ON incident_comments(incident_id, created_at);

CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    scope VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    resource_id UUID,
    response_status INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency_scope_key UNIQUE(organization_id, scope, idempotency_key)
);

CREATE TABLE webhook_deliveries (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    source VARCHAR(32) NOT NULL,
    delivery_id VARCHAR(255) NOT NULL,
    nonce VARCHAR(255) NOT NULL,
    signature_valid BOOLEAN NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    incident_id UUID,
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_webhook_delivery UNIQUE(organization_id, source, delivery_id),
    CONSTRAINT uq_webhook_nonce UNIQUE(organization_id, source, nonce)
);
