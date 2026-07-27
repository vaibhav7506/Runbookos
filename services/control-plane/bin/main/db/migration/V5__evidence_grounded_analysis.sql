CREATE TABLE ai_provider_configs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(120) NOT NULL,
    base_url VARCHAR(512),
    encrypted_api_key TEXT,
    key_nonce TEXT,
    key_id VARCHAR(64),
    key_fingerprint VARCHAR(64),
    priority INTEGER NOT NULL DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    timeout_seconds INTEGER NOT NULL DEFAULT 30,
    max_retries INTEGER NOT NULL DEFAULT 1,
    input_token_budget INTEGER NOT NULL DEFAULT 12000,
    output_token_budget INTEGER NOT NULL DEFAULT 2000,
    cost_budget_usd NUMERIC(12,6) NOT NULL DEFAULT 1.000000,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_ai_provider_priority UNIQUE (organization_id, priority),
    CONSTRAINT ck_ai_provider_budget CHECK (
        timeout_seconds BETWEEN 1 AND 120
        AND max_retries BETWEEN 0 AND 3
        AND input_token_budget BETWEEN 256 AND 100000
        AND output_token_budget BETWEEN 128 AND 16000
        AND cost_budget_usd >= 0
    )
);

CREATE INDEX idx_ai_provider_org ON ai_provider_configs(organization_id, enabled, priority);

CREATE TABLE incident_analyses (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    incident_id UUID NOT NULL REFERENCES incidents(id),
    status VARCHAR(24) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(120) NOT NULL,
    prompt_key VARCHAR(80) NOT NULL,
    prompt_version INTEGER NOT NULL,
    output JSONB NOT NULL,
    quality JSONB NOT NULL,
    latency_ms BIGINT NOT NULL,
    input_tokens INTEGER NOT NULL,
    output_tokens INTEGER NOT NULL,
    estimated_cost_usd NUMERIC(12,6) NOT NULL,
    fallback_reason VARCHAR(512),
    requested_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_incident_analysis_status CHECK (status IN ('COMPLETED', 'REJECTED')),
    CONSTRAINT ck_incident_analysis_usage CHECK (
        latency_ms >= 0 AND input_tokens >= 0 AND output_tokens >= 0
        AND estimated_cost_usd >= 0
    )
);

CREATE INDEX idx_analysis_incident ON incident_analyses(organization_id, incident_id, created_at DESC);

CREATE TABLE ai_provider_failures (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    provider VARCHAR(32) NOT NULL,
    failure_count INTEGER NOT NULL,
    circuit_open_until TIMESTAMPTZ,
    last_failure_reason VARCHAR(512),
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_ai_provider_failure UNIQUE (organization_id, provider)
);
