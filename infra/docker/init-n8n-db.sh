#!/bin/bash
# Creates the n8n database on the shared PostgreSQL instance.
# Mounted at /docker-entrypoint-initdb.d/ and executed automatically.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE n8n' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'n8n')\gexec
EOSQL
