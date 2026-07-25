# n8n Workflows — RunbookOS

This directory contains exported n8n workflow JSON files used by RunbookOS.

## Structure

```
automation/n8n/
  workflows/          # Exported workflow JSON files
  credentials-example/ # Example credential configurations (no secrets)
  README.md           # This file
```

## Workflow Management

Workflows are version-controlled as JSON exports. When making changes:

1. Edit workflows in the n8n UI
2. Export the workflow JSON
3. Place it in the `workflows/` directory
4. Commit with a clear description

## Security

- Workflows execute actions gated by Spring Boot signed tokens
- n8n never makes authorization decisions
- All execution requests include expiry and nonce
- Callbacks include verifiable HMAC signatures
- Credentials are managed through n8n's encrypted credential store

## Available Workflows

Workflows will be added in Phase 4 (n8n Orchestration and Execution Engine).

## n8n Configuration

n8n is configured through Docker Compose with:
- PostgreSQL-backed persistence
- Basic authentication enabled
- Encryption key for credential storage

See the root `docker-compose.yml` and `.env.example` for configuration details.
