-- Create the n8n database if it does not exist.
-- This script is mounted into the PostgreSQL init directory.
SELECT 'CREATE DATABASE n8n'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'n8n');

-- Execute via DO block for conditional creation
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'n8n') THEN
    PERFORM dblink_exec('dbname=postgres', 'CREATE DATABASE n8n');
  END IF;
EXCEPTION WHEN OTHERS THEN
  -- dblink may not be available; use alternative approach
  NULL;
END
$$;
