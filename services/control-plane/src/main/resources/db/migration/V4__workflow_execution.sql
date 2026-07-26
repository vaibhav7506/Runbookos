CREATE TABLE executions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    workflow_key VARCHAR(96) NOT NULL,
    workflow_version INTEGER NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    allowed_action_ids JSONB NOT NULL,
    token_nonce VARCHAR(128) NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    timeout_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_execution_idempotency UNIQUE(organization_id,idempotency_key),
    CONSTRAINT uq_execution_nonce UNIQUE(token_nonce),
    CONSTRAINT ck_execution_status CHECK(status IN('QUEUED','RUNNING','PAUSED_FOR_APPROVAL','SUCCEEDED','PARTIALLY_SUCCEEDED','FAILED','TIMED_OUT','CANCELLED'))
);
CREATE INDEX ix_executions_org_created ON executions(organization_id,created_at DESC);
CREATE INDEX ix_executions_incident ON executions(organization_id,incident_id,created_at);
CREATE INDEX ix_executions_timeout ON executions(status,timeout_at);

CREATE TABLE step_executions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    execution_id UUID NOT NULL REFERENCES executions(id) ON DELETE CASCADE,
    action_id VARCHAR(96) NOT NULL,
    name VARCHAR(160) NOT NULL,
    sequence_number INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt INTEGER NOT NULL DEFAULT 1,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_code VARCHAR(96),
    error_message VARCHAR(512),
    output JSONB NOT NULL DEFAULT '{}'::jsonb,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_step_sequence_attempt UNIQUE(execution_id,sequence_number,attempt),
    CONSTRAINT ck_step_status CHECK(status IN('QUEUED','RUNNING','WAITING','APPROVED','DENIED','SUCCEEDED','FAILED','SKIPPED'))
);
CREATE INDEX ix_steps_execution_sequence ON step_executions(execution_id,sequence_number,attempt);

CREATE TABLE execution_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    execution_id UUID NOT NULL REFERENCES executions(id) ON DELETE CASCADE,
    step_execution_id UUID,
    event_type VARCHAR(64) NOT NULL,
    status VARCHAR(32),
    message VARCHAR(512) NOT NULL,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_execution_events_time ON execution_events(execution_id,occurred_at,id);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(96) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(24) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    claimed_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    last_error VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_outbox_status CHECK(status IN('PENDING','PROCESSING','DELIVERED','DEAD'))
);
CREATE INDEX ix_outbox_pending ON outbox_events(status,next_attempt_at,created_at);

CREATE TABLE workflow_callback_nonces (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL REFERENCES executions(id) ON DELETE CASCADE,
    nonce VARCHAR(128) NOT NULL UNIQUE,
    received_at TIMESTAMPTZ NOT NULL
);
