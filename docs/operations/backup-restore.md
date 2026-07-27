# Backup and restore

## Back up

Store backups encrypted, access controlled, and outside the application host.

```bash
docker compose exec -T postgres pg_dump -U runbookos -Fc runbookos > runbookos.dump
docker compose exec -T postgres pg_dump -U runbookos -Fc n8n > n8n.dump
docker run --rm -v runbookos_n8n_data:/source:ro -v "${PWD}:/backup" alpine \
  tar -czf /backup/n8n-data.tgz -C /source .
```

Back up the exact `N8N_ENCRYPTION_KEY` and the RunbookOS encryption-key material in a secret manager. Database dumps alone cannot decrypt stored credentials.

## Restore rehearsal

Stop writers, restore into new empty databases, and validate before switching traffic:

```bash
docker compose stop control-plane n8n
docker compose exec -T postgres createdb -U runbookos runbookos_restore
docker compose exec -T postgres pg_restore -U runbookos -d runbookos_restore --clean --if-exists < runbookos.dump
```

Start a control-plane instance against the restored database, let Flyway validate/migrate it, check `/api/health`, verify tenant counts and recent audit hashes, then test login, Demo Mode, and a non-destructive workflow. Document recovery time and recovery point achieved during every rehearsal.
