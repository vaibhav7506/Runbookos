ALTER TABLE executions
    ADD COLUMN archived_at TIMESTAMPTZ;

ALTER TABLE incidents
    ADD COLUMN archived_at TIMESTAMPTZ;

CREATE INDEX idx_incidents_active
    ON incidents (organization_id, status, last_signal_at DESC)
    WHERE archived_at IS NULL;

CREATE INDEX idx_executions_active
    ON executions (organization_id, status, created_at DESC)
    WHERE archived_at IS NULL;

CREATE INDEX idx_outbox_dead_letters
    ON outbox_events (organization_id, status, created_at DESC)
    WHERE status = 'DEAD';

ALTER TABLE audit_events
    ADD COLUMN previous_hash VARCHAR(64),
    ADD COLUMN event_hash VARCHAR(64);

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION chain_audit_event()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    prior VARCHAR(64);
BEGIN
    SELECT event_hash INTO prior
      FROM audit_events
     WHERE organization_id IS NOT DISTINCT FROM NEW.organization_id
     ORDER BY occurred_at DESC, id DESC
     LIMIT 1;
    NEW.previous_hash := COALESCE(prior, repeat('0', 64));
    NEW.event_hash := encode(
        digest(
            NEW.previous_hash || NEW.id::text || NEW.action || NEW.resource_type ||
            COALESCE(NEW.resource_id, '') || NEW.outcome || NEW.occurred_at::text ||
            NEW.metadata::text,
            'sha256'
        ),
        'hex'
    );
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_audit_hash_chain
BEFORE INSERT ON audit_events
FOR EACH ROW EXECUTE FUNCTION chain_audit_event();

UPDATE audit_events
SET previous_hash = repeat('0', 64),
    event_hash = encode(
        digest(
            repeat('0', 64) || id::text || action || resource_type ||
            COALESCE(resource_id, '') || outcome || occurred_at::text || metadata::text,
            'sha256'
        ),
        'hex'
    )
WHERE event_hash IS NULL;

ALTER TABLE audit_events
    ALTER COLUMN previous_hash SET NOT NULL,
    ALTER COLUMN event_hash SET NOT NULL;

CREATE INDEX idx_audit_integrity
    ON audit_events (organization_id, occurred_at DESC, event_hash);
