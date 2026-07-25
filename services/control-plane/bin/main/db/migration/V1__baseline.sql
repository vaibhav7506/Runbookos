-- RunbookOS Baseline Migration
-- This initial migration creates the schema version tracking.
-- Actual domain tables will be added in Phase 2.

-- Ensure UUID extension is available
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
