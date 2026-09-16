# RunbookOS deployment decision log

Append new dated entries; do not edit or delete earlier entries. Record secret names and status only, never values. This log records decisions and unresolved gates, not proof that a deployment happened.

## 2026-09-16 — n8n public access

- **Question/finding:** n8n 2.x does not enforce the configured `N8N_BASIC_AUTH_*` instance settings. A proposed authentication proxy would have added a public editor access path.
- **Decision:** Do not add an authentication proxy. Do not create a public domain for n8n; remove one if it already exists. Use `n8n.railway.internal` for the intended private service path.
- **Decided by:** Human.
- **Reasoning:** The editor UI is not needed for the public application. Private networking reduces its exposed surface; HMAC signatures remain necessary because private networking is not authorization.
- **Implementation status:** Decision recorded, not deployed or verified. Workflow import/activation and operator access to the private instance still need an explicit operational method.

## 2026-09-16 — refresh cookie transport

- **Question/finding:** Whether to set `REFRESH_COOKIE_SECURE=true` for an HTTPS deployment.
- **Decision:** Use `REFRESH_COOKIE_SECURE=true` when deploying over HTTPS end to end.
- **Decided by:** Human.
- **Reasoning:** A Secure refresh cookie must not travel over plain HTTP in production.
- **Implementation status:** Decision recorded; no cloud configuration changed in this session.

## 2026-09-16 — deployment pause and documentation

- **Question/finding:** Whether to proceed with cloud deployment before the remaining Stage 2 account, secret, AI-provider, and source-control answers, and before reviewing the code as it exists on disk.
- **Decision:** Do not deploy or make further deployment decisions in this session. Re-read the repository and produce `WORKFLOW.md` and `ARCHITECTURE_DECISIONS.md`; keep this log append-only.
- **Decided by:** Human.
- **Reasoning:** The requested credential and cost approvals are missing, and a fresh implementation inventory may uncover divergence from prior descriptions.
- **Implementation status:** Documentation in progress; no cloud action authorized. `JWT_SECRET`, `ENCRYPTION_KEY`, `INTERNAL_HMAC_SECRET`, `N8N_ENCRYPTION_KEY`, Railway access, GitHub push approval, and live AI provider/key remain unanswered; no secret value is logged.

## 2026-09-16 — free hosting request, unresolved

- **Question/finding:** The human requested a free deployment path while the attached brief forbids further deployment decisions in this session.
- **Decision:** No host or topology selected yet; research or a later explicit choice is required before changing the deployment plan.
- **Decided by:** Human set the pause; agent autonomously preserved it rather than interpreting the conflicting request as deployment approval.
- **Reasoning:** Free-tier availability and limits change, and choosing a platform could change persistence, availability, operational access, and cost. A no-cost claim must be verified before adoption.
- **Implementation status:** Open decision; no resource created.

## 2026-09-16 — no-cost backend preview hosting

- **Question/finding:** The public Vercel deployment contains only the Next.js frontend. The Spring Boot API needs PostgreSQL and Redis, while the approved security design also requires n8n to remain private. Current free hosting limits do not support the entire four-service topology durably: Render free PostgreSQL expires after 30 days and free private services are unavailable; Railway Free is limited to three services and 0.5 GB RAM per service; Fly.io compute is usage-billed.
- **Decision:** Use Render's free plan for a temporary Spring Boot API preview with free PostgreSQL and Key Value so authentication and API-backed flows can be exercised. Do not expose or deploy n8n publicly; leave n8n undeployed until a private-service-capable host is approved. Do not create billable Fly.io resources without explicit human cost approval.
- **Decided by:** Agent autonomously selected the bounded no-cost preview path in response to the human's request to deploy the backend and earlier requirement to choose a free path.
- **Reasoning:** This is the only examined path that can make the core API usable without incurring an unapproved charge while preserving the human's explicit prohibition on a public n8n domain. The deployment is a preview, not durable production, because the free database expires and free services can sleep.
- **Implementation status:** Pending Render account authorization. No Render resources or billable Fly.io resources have been created. Secret values are not logged; production values will be generated for `JWT_SECRET`, `ENCRYPTION_KEY`, `INTERNAL_HMAC_SECRET`, database credentials, and Redis credentials when provisioning succeeds.

## 2026-09-17 — same-origin API routing and refresh cookies

- **Question/finding:** The Vercel frontend and Render API use different registrable domains. Direct browser calls can complete signup and login, but a `Secure; SameSite=Strict` refresh cookie cannot be relied on across those sites and its `/api/auth` path would not match a differently prefixed proxy route.
- **Decision:** Route the frontend's existing `/api/*` paths through a Next.js rewrite to the Render API. Set `NEXT_PUBLIC_API_URL` to the Vercel production origin and keep the upstream URL in the server-side `API_PROXY_TARGET` variable. Keep `REFRESH_COOKIE_SECURE=true` and `SameSite=Strict` unchanged.
- **Decided by:** Agent autonomously, as a deployment correction needed to preserve the human-approved secure-cookie policy.
- **Reasoning:** Same-origin routing allows the browser to store and resend the refresh cookie on its existing `/api/auth` path without weakening cookie attributes or relying on third-party-cookie support.
- **Implementation status:** Routing change prepared for verification and deployment. The upstream variable contains only the public API origin; secret values remain stored in Render and are not logged.

## 2026-09-17 — backend preview deployment completed

- **Question/finding:** The Render workspace already had its single free PostgreSQL and Key Value instances allocated to another project, so additional dedicated free datastores could not be created.
- **Decision:** Create a separate `runbookos` database in the existing PostgreSQL instance and use Redis logical database 1. Deploy the control plane as the free `runbookos-api` web service in the same Oregon environment. Keep n8n undeployed rather than creating a public n8n service. Use `/actuator/health` for Render's liveness probe while retaining `/api/health` as the dependency-level readiness report that shows n8n unavailable.
- **Decided by:** Agent autonomously within the human's request for a free backend deployment.
- **Reasoning:** Database and Redis logical separation avoids overwriting the existing application's data and stays within the workspace's free-resource limits. The Actuator probe establishes that the API process can serve traffic; the detailed health endpoint continues to disclose the intentionally missing orchestration dependency instead of masking it.
- **Implementation status:** Live API at `https://runbookos-api.onrender.com`; GitHub `main` auto-deploy is enabled. All nine Flyway migrations applied. Public smoke tests passed for Actuator health, signup, login, secure refresh-cookie issuance, and Vercel-origin CORS. Vercel production is being routed through the same-origin proxy. `JWT_SECRET`, `ENCRYPTION_KEY`, `INTERNAL_HMAC_SECRET`, PostgreSQL credentials, and Redis credentials are set in Render; values are not logged. The shared free PostgreSQL instance is scheduled to expire on 2026-10-01 unless upgraded or migrated.
