# Deployment

## Profiles

- `dev`: local Java and Next.js processes with Docker infrastructure.
- `docker`: the complete local Compose stack.
- `staging`: secure cookies, detailed application diagnostics, and higher trace sampling.
- `prod`: secure cookies, restrained logs, lower trace sampling, and Swagger UI disabled.

## Production Compose

Copy `.env.production.example` to a protected environment file, replace every value, place `fullchain.pem` and `privkey.pem` in `TLS_CERT_DIR`, then validate and start:

```bash
docker compose --env-file .env.production -f docker-compose.production.yml config
docker compose --env-file .env.production -f docker-compose.production.yml up -d --build
```

Add `--profile observability` to start Prometheus and Grafana. The production example uses private networking, no public database or Redis ports, non-root application images, health checks, graceful shutdown, TLS termination, an n8n queue worker, and required-secret interpolation.

Flyway migrations run before Hibernate validation during control-plane startup. Take a verified database backup before deployment, deploy one control-plane instance to migrate, verify `/api/health`, then roll out remaining instances. Never edit an applied migration.

Use a managed load balancer or the included nginx example. Terminate TLS 1.2 or later, redirect HTTP to HTTPS, preserve forwarded headers, disable response buffering for SSE, and restrict actuator/metrics access at the network layer.

For staging, use an isolated database, credentials, encryption keys, webhook URLs, and provider accounts. Never point staging at production integrations.
