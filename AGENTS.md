# RunbookOS — Agent Instructions

This file contains repository-specific instructions for coding agents working on RunbookOS.

## Project Overview

RunbookOS is a Human-Governed AI Incident Response Platform. It receives production incident signals, collects diagnostic evidence, generates AI-grounded analysis, recommends runbooks, and executes remediation with human approval.

## Architecture

- **Backend**: Java 21, Spring Boot 4.1.x, Gradle Kotlin DSL
- **Frontend**: Next.js 16.x, TypeScript strict, Tailwind CSS
- **Orchestration**: Self-hosted n8n with PostgreSQL persistence
- **Database**: PostgreSQL 16
- **Cache**: Redis 7.4
- **Infrastructure**: Docker Compose

## Repository Structure

```
runbookos/
  apps/web/                    # Next.js frontend application
  services/control-plane/      # Spring Boot backend service
  automation/n8n/              # n8n workflow definitions
  packages/api-client/         # Generated TypeScript API client
  infra/docker/                # Docker build configs
  infra/observability/         # Prometheus/Grafana configs
  docs/                        # Documentation
  scripts/                     # Development scripts
  .github/workflows/           # CI/CD pipelines
```

## Critical Rules

### Security
- Never expose JPA entities through controllers. Use DTOs.
- All organization-owned data must include organization isolation.
- Never return secret values after creation.
- Never hardcode secrets. Use environment variables.
- AI is advisory only — the Java policy engine makes authorization decisions.
- External content (logs, commits, tickets) is untrusted input.
- Model output is untrusted — always validate structured output.

### Backend Conventions
- Root package: `com.vaibhav.runbookos`
- Constructor injection only (no field injection).
- Immutable DTOs where practical.
- UUID identifiers for all entities.
- UTC timestamps internally.
- Optimistic locking where concurrent modification could occur.
- Structured domain errors (code, message, status, correlationId).
- Controllers must remain thin — business logic in services.
- Flyway for database migrations (never modify released migrations).
- Use Jakarta EE 11 namespace (`jakarta.*`).

### Frontend Conventions
- TypeScript strict mode — no `any` unless technically unavoidable.
- Generated API client from OpenAPI — never duplicate backend types manually.
- Server/client component boundaries must be deliberate.
- Accessible primitives (Radix UI).
- Handle cancellation and stale responses (TanStack Query).
- Do not expose secrets in browser bundles.

### Testing
- JUnit 5 + Mockito + AssertJ for backend.
- Testcontainers for integration tests.
- WireMock for external API tests.
- Tests must be deterministic — no external API calls in CI.

### n8n Workflows
- Keep workflows small and composable.
- Use sub-workflows.
- Name every node clearly.
- Error branches on all workflows.
- Version-control exported workflow JSON.
- Never treat workflow configuration as authorization.

## Design System

- Follow the Apple-inspired design direction: quiet, premium, restrained, functional.
- Use the defined color palette (see apps/web/src/styles/globals.css).
- System font stack — no proprietary fonts.
- Transitions 140–240ms. No flashy animations.
- WCAG 2.2 AA accessibility target.
- Desktop-first with responsive tablet/mobile.

## Phases

The project is built in 10 sequential phases. Check PROJECT_PLAN.md for current status.
Do not skip phases or begin the next phase before the current one passes acceptance criteria.
