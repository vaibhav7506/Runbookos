# RunbookOS — Project Plan

## Current Phase: 1 — Foundation, Architecture, and Design System

## Phase Status

| Phase | Name | Status |
|:------|:-----|:-------|
| 1 | Foundation, Architecture, and Design System | 🔄 In Progress |
| 2 | Identity, Multi-Tenancy, Security, and Data Model | ⬜ Not Started |
| 3 | Incident Ingestion, Deduplication, and Triage | ⬜ Not Started |
| 4 | n8n Orchestration and Execution Engine | ⬜ Not Started |
| 5 | Evidence-Grounded AI Incident Analysis | ⬜ Not Started |
| 6 | Runbooks, Policy Engine, and Human Approval | ⬜ Not Started |
| 7 | Integrations and Complete Incident Workflow | ⬜ Not Started |
| 8 | Reliability, Security, and Observability | ⬜ Not Started |
| 9 | Apple-Level Product Polish and Complete UX | ⬜ Not Started |
| 10 | Testing, Deployment, Documentation, and Final Audit | ⬜ Not Started |

## Phase 1 — Completed Tasks

- [ ] Initialize monorepo structure
- [ ] Create Spring Boot control-plane service
- [ ] Create Next.js web application
- [ ] Add Docker Compose (PostgreSQL, Redis, n8n)
- [ ] Add health endpoints
- [ ] Configure environment validation
- [ ] Add formatting and linting
- [ ] Create base design system
- [ ] Implement light and dark themes
- [ ] Build application shell
- [ ] Create favicon
- [ ] Create landing page
- [ ] Add documentation (README, AGENTS, ARCHITECTURE, CONTRIBUTING, SECURITY)
- [ ] Add Mermaid diagrams
- [ ] Add root Makefile and scripts

## Architectural Decisions

| ID | Decision | Rationale |
|:---|:---------|:----------|
| ADR-001 | Spring Boot 4.1.0 with Java 21 | Latest stable, Jakarta EE 11, full Java 21 support |
| ADR-002 | Gradle Kotlin DSL | Type-safe build configuration, IDE support |
| ADR-003 | Next.js 16.x with App Router | Latest stable, RSC support, built-in optimizations |
| ADR-004 | Tailwind CSS 4.x | Specified in requirements, utility-first CSS |
| ADR-005 | PostgreSQL 16 shared instance | Separate databases for app and n8n on same instance |
| ADR-006 | MIT License | Permissive open-source license |

## Unresolved Risks

| Risk | Severity | Mitigation |
|:-----|:---------|:-----------|
| Spring Boot 4.1.0 ecosystem maturity | Medium | Fallback to 4.0.7 if critical dependency incompatibility |
| n8n Docker networking | Low | Standard compose networking, tested in Phase 1 |

## Test Status

| Suite | Status | Notes |
|:------|:-------|:------|
| Backend compile | ⬜ Pending | |
| Backend unit tests | ⬜ Pending | |
| Frontend lint | ⬜ Pending | |
| Frontend type-check | ⬜ Pending | |
| Frontend build | ⬜ Pending | |
| Docker Compose | ⬜ Pending | |

## Next Phase

Phase 2 — Identity, Multi-Tenancy, Security, and Data Model
