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
