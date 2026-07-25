# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in RunbookOS, please report it responsibly.

**Do not open a public GitHub issue for security vulnerabilities.**

Instead, please email security concerns to the repository maintainer with:

1. A description of the vulnerability
2. Steps to reproduce
3. Potential impact assessment
4. Suggested fix (if any)

## Security Model

RunbookOS enforces a strict security model:

- **AI is advisory only** — The AI may recommend actions but never has final authority over authorization, permissions, policy evaluation, tenant access, destructive execution, production rollback, or secret handling.
- **Java policy engine** — All authorization decisions are made by the Java backend policy engine.
- **Human-in-the-loop** — Reversible actions require human approval. High-risk actions require elevated approval from multiple authorized users.
- **Organization isolation** — All data is scoped to organizations. Cross-tenant access is prevented at the service and repository layers.
- **Immutable audit trail** — Every state transition, approval decision, and action execution is recorded in an immutable audit log.
- **Secret handling** — Secrets are encrypted at rest using authenticated encryption. Secret values are never returned after creation. Secrets are redacted before AI model calls.

## Supported Versions

| Version | Supported |
|:--------|:----------|
| Latest  | ✅        |

## Security Controls

See [docs/security/](docs/security/) for detailed documentation on:

- Threat model
- Approval model
- Secret handling
- Trust boundaries
- SSRF protections
- Rate limiting
