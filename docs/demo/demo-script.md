# Five-minute recruiter demo

## 0:00–0:30 — Problem and product

Explain that incident tools often automate investigation and remediation without keeping authorization, evidence, and accountability in one place. RunbookOS gives responders fast AI-assisted analysis while retaining deterministic human control.

## 0:30–1:15 — Architecture

Show the README architecture diagram. Spring Boot owns tenant access, policy, state, approvals, and audit. n8n performs bounded orchestration using short-lived signed dispatches. PostgreSQL is authoritative; Redis supports coordination.

## 1:15–2:15 — Trigger and evidence

Create a Demo Mode organization, launch the seeded checkout incident, and open the incident workspace. Expand evidence to show deployments, health signals, and logs without requiring paid providers.

## 2:15–3:15 — Grounded analysis

Run AI analysis. Point out evidence identifiers, confidence, missing information, hypotheses, provider usage, and the advisory-only label.

## 3:15–4:15 — Policy and approval

Open a runbook and its policy preview. Contrast read-only diagnostics, reversible actions, elevated actions, and prohibited operations. Review an approval’s policy reason and identity-bound decision.

## 4:15–5:00 — Audit and operations

Show the execution timeline, operational health, hash-chained audit log, and printable postmortem. Finish with the CI pipeline, deterministic test suite, Prometheus metrics, Grafana dashboard, and production Compose topology.
