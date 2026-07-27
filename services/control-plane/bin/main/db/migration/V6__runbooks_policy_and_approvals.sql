CREATE TABLE runbooks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(160) NOT NULL,
    description TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_runbook_name UNIQUE (organization_id, name)
);

CREATE TABLE runbook_versions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    runbook_id UUID NOT NULL REFERENCES runbooks(id),
    version_number INTEGER NOT NULL,
    status VARCHAR(24) NOT NULL,
    change_summary VARCHAR(512),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    published_by UUID REFERENCES users(id),
    published_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_runbook_version UNIQUE (runbook_id, version_number),
    CONSTRAINT ck_runbook_version_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'SUPERSEDED'))
);

CREATE TABLE runbook_steps (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    runbook_version_id UUID NOT NULL REFERENCES runbook_versions(id) ON DELETE CASCADE,
    step_key VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    sequence_number INTEGER NOT NULL,
    step_type VARCHAR(32) NOT NULL,
    risk_classification VARCHAR(24) NOT NULL,
    required_role VARCHAR(24) NOT NULL,
    timeout_seconds INTEGER NOT NULL,
    max_retries INTEGER NOT NULL,
    rollback_information TEXT,
    allowed_environments JSONB NOT NULL,
    configuration JSONB NOT NULL,
    CONSTRAINT uq_runbook_step_key UNIQUE (runbook_version_id, step_key),
    CONSTRAINT uq_runbook_step_sequence UNIQUE (runbook_version_id, sequence_number),
    CONSTRAINT ck_runbook_step_type CHECK (step_type IN (
        'HTTP_REQUEST', 'N8N_WORKFLOW', 'GITHUB_ACTION', 'NOTIFICATION',
        'MANUAL_TASK', 'WAIT', 'CONDITION', 'AI_RECOMMENDATION'
    )),
    CONSTRAINT ck_runbook_step_risk CHECK (risk_classification IN (
        'READ_ONLY', 'REVERSIBLE', 'HIGH_RISK', 'PROHIBITED'
    )),
    CONSTRAINT ck_runbook_step_retry CHECK (timeout_seconds BETWEEN 1 AND 3600 AND max_retries BETWEEN 0 AND 5)
);

CREATE INDEX idx_runbook_org ON runbooks(organization_id, updated_at DESC);
CREATE INDEX idx_runbook_version ON runbook_versions(organization_id, runbook_id, version_number DESC);

CREATE OR REPLACE FUNCTION reject_published_runbook_step_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    target_version UUID;
    target_status VARCHAR(24);
BEGIN
    target_version := CASE WHEN TG_OP = 'DELETE' THEN OLD.runbook_version_id ELSE NEW.runbook_version_id END;
    SELECT status INTO target_status FROM runbook_versions WHERE id = target_version;
    IF target_status <> 'DRAFT' THEN
        RAISE EXCEPTION 'Published runbook version steps are immutable';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$;

CREATE TRIGGER trg_runbook_steps_draft_only
BEFORE INSERT OR UPDATE OR DELETE ON runbook_steps
FOR EACH ROW EXECUTE FUNCTION reject_published_runbook_step_mutation();

CREATE OR REPLACE FUNCTION reject_published_runbook_version_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' AND OLD.status <> 'DRAFT' THEN
        RAISE EXCEPTION 'Published runbook versions are immutable';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status <> 'DRAFT' THEN
        IF NOT (
            OLD.status = 'PUBLISHED'
            AND NEW.status = 'SUPERSEDED'
            AND (to_jsonb(NEW) - 'status' - 'row_version')
                = (to_jsonb(OLD) - 'status' - 'row_version')
        ) THEN
            RAISE EXCEPTION 'Published runbook versions are immutable';
        END IF;
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$;

CREATE TRIGGER trg_runbook_versions_immutable
BEFORE UPDATE OR DELETE ON runbook_versions
FOR EACH ROW EXECUTE FUNCTION reject_published_runbook_version_mutation();

CREATE TABLE policies (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(120) NOT NULL,
    environment VARCHAR(24) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_policy_environment UNIQUE (organization_id, environment)
);

CREATE TABLE policy_rules (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    policy_id UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    risk_classification VARCHAR(24) NOT NULL,
    decision VARCHAR(32) NOT NULL,
    required_approvals INTEGER NOT NULL,
    approver_roles JSONB NOT NULL,
    typed_confirmation_required BOOLEAN NOT NULL,
    expiration_minutes INTEGER NOT NULL,
    CONSTRAINT uq_policy_risk UNIQUE (policy_id, risk_classification),
    CONSTRAINT ck_policy_decision CHECK (decision IN (
        'ALLOW', 'REQUIRE_APPROVAL', 'REQUIRE_ELEVATED_APPROVAL', 'DENY'
    )),
    CONSTRAINT ck_policy_approvals CHECK (required_approvals BETWEEN 0 AND 5),
    CONSTRAINT ck_policy_expiry CHECK (expiration_minutes BETWEEN 1 AND 1440)
);

CREATE TABLE policy_decision_logs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    policy_id UUID REFERENCES policies(id),
    incident_id UUID REFERENCES incidents(id),
    runbook_version_id UUID REFERENCES runbook_versions(id),
    step_key VARCHAR(80) NOT NULL,
    environment VARCHAR(24) NOT NULL,
    risk_classification VARCHAR(24) NOT NULL,
    decision VARCHAR(32) NOT NULL,
    reason TEXT NOT NULL,
    actor_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_policy_decision_org ON policy_decision_logs(organization_id, created_at DESC);

CREATE TABLE approval_requests (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    incident_id UUID REFERENCES incidents(id),
    execution_id UUID REFERENCES executions(id),
    runbook_version_id UUID REFERENCES runbook_versions(id),
    step_key VARCHAR(80) NOT NULL,
    action_name VARCHAR(160) NOT NULL,
    environment VARCHAR(24) NOT NULL,
    risk_classification VARCHAR(24) NOT NULL,
    policy_decision VARCHAR(32) NOT NULL,
    policy_reason TEXT NOT NULL,
    status VARCHAR(24) NOT NULL,
    required_approvals INTEGER NOT NULL,
    confirmation_phrase VARCHAR(160),
    requested_by UUID NOT NULL REFERENCES users(id),
    requested_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    cancellation_reason VARCHAR(512),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_approval_status CHECK (status IN (
        'PENDING', 'APPROVED', 'DENIED', 'EXPIRED', 'CANCELLED'
    ))
);

CREATE TABLE approval_decisions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    approval_request_id UUID NOT NULL REFERENCES approval_requests(id),
    approver_user_id UUID NOT NULL REFERENCES users(id),
    decision VARCHAR(16) NOT NULL,
    reason VARCHAR(1000),
    decided_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_approval_approver UNIQUE (approval_request_id, approver_user_id),
    CONSTRAINT ck_approval_decision CHECK (decision IN ('APPROVE', 'DENY'))
);

CREATE INDEX idx_approval_inbox ON approval_requests(organization_id, status, requested_at DESC);
CREATE INDEX idx_approval_incident ON approval_requests(organization_id, incident_id, status);

CREATE TABLE approval_callback_nonces (
    id UUID PRIMARY KEY,
    nonce VARCHAR(128) NOT NULL UNIQUE,
    received_at TIMESTAMPTZ NOT NULL
);
