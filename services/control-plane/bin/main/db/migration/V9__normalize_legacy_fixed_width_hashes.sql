-- Hibernate 7 validates Java String fields as VARCHAR. Preserve existing values while
-- normalizing the fixed-width Phase 2 hash column through a forward-only migration.
ALTER TABLE refresh_sessions
    ALTER COLUMN token_hash TYPE VARCHAR(64);
