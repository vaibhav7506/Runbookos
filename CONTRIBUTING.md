# Contributing to RunbookOS

Thank you for considering contributing to RunbookOS.

## Development Setup

### Prerequisites

- Java 21 (Eclipse Temurin recommended)
- Node.js 22 LTS
- Docker and Docker Compose v2
- Git

### Local Development

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/runbookos.git
   cd runbookos
   ```

2. Copy environment configuration:
   ```bash
   cp .env.example .env
   ```

3. Start infrastructure services:
   ```bash
   docker compose up -d postgres redis n8n
   ```

4. Start the backend:
   ```bash
   cd services/control-plane
   ./gradlew bootRun
   ```

5. Start the frontend:
   ```bash
   cd apps/web
   npm install
   npm run dev
   ```

## Code Style

### Java (Backend)

- Google Java Format enforced via Spotless
- Run `./gradlew spotlessApply` to format
- Run `./gradlew spotlessCheck` to verify
- Constructor injection only (no field injection)
- Immutable DTOs where practical
- Meaningful names over excessive comments

### TypeScript (Frontend)

- TypeScript strict mode enabled
- Prettier for formatting
- ESLint for linting
- Run `npm run lint` to check
- Run `npm run format` to auto-format
- No `any` types unless technically unavoidable and documented

### General

- Keep controllers thin; business logic in services
- Use DTOs for API boundaries — never expose JPA entities
- All organization-owned data must include organization isolation
- UTC timestamps internally
- UUID identifiers

## Pull Request Process

1. Create a feature branch from `main`
2. Make your changes
3. Run all checks:
   - Backend: `./gradlew build test spotlessCheck`
   - Frontend: `npm run lint && npm run type-check && npm run build`
4. Write meaningful commit messages
5. Open a PR with a clear description
6. Ensure CI passes

## Reporting Issues

- Use GitHub Issues for bug reports and feature requests
- For security vulnerabilities, see [SECURITY.md](SECURITY.md)
