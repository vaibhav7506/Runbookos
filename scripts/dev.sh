#!/usr/bin/env bash
# RunbookOS development scripts
# Usage: ./scripts/dev.sh <command>

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  echo "Usage: $0 <command>"
  echo ""
  echo "Commands:"
  echo "  infra        Start infrastructure (postgres, redis, n8n)"
  echo "  infra:down   Stop infrastructure"
  echo "  backend      Start Spring Boot control plane"
  echo "  frontend     Start Next.js web application"
  echo "  all          Start all services via Docker Compose"
  echo "  all:down     Stop all Docker Compose services"
  echo "  test         Run all tests"
  echo "  test:backend Run backend tests only"
  echo "  test:frontend Run frontend tests only"
  echo "  lint         Run all linters"
  echo "  format       Run all formatters"
  echo "  build        Run all builds"
  echo "  health       Check health of all services"
  echo ""
}

case "${1:-}" in
  infra)
    echo "→ Starting infrastructure services..."
    docker compose -f "$ROOT/docker-compose.yml" up -d postgres redis n8n
    echo "✓ Infrastructure started"
    ;;

  infra:down)
    echo "→ Stopping infrastructure services..."
    docker compose -f "$ROOT/docker-compose.yml" stop postgres redis n8n
    echo "✓ Infrastructure stopped"
    ;;

  backend)
    echo "→ Starting Spring Boot control plane..."
    cd "$ROOT/services/control-plane"
    ./gradlew bootRun
    ;;

  frontend)
    echo "→ Starting Next.js web application..."
    cd "$ROOT/apps/web"
    npm run dev
    ;;

  all)
    echo "→ Starting all services..."
    docker compose -f "$ROOT/docker-compose.yml" up
    ;;

  all:down)
    echo "→ Stopping all services..."
    docker compose -f "$ROOT/docker-compose.yml" down
    ;;

  test)
    echo "→ Running all tests..."
    "$0" test:backend
    "$0" test:frontend
    echo "✓ All tests complete"
    ;;

  test:backend)
    echo "→ Running backend tests..."
    cd "$ROOT/services/control-plane"
    ./gradlew test
    echo "✓ Backend tests complete"
    ;;

  test:frontend)
    echo "→ Running frontend checks..."
    cd "$ROOT/apps/web"
    npm run lint
    npm run type-check
    npm test
    echo "✓ Frontend checks complete"
    ;;

  lint)
    echo "→ Checking Java formatting..."
    cd "$ROOT/services/control-plane"
    ./gradlew spotlessCheck
    echo "→ Checking TypeScript..."
    cd "$ROOT/apps/web"
    npm run lint
    npm run type-check
    echo "✓ Lint complete"
    ;;

  format)
    echo "→ Formatting Java..."
    cd "$ROOT/services/control-plane"
    ./gradlew spotlessApply
    echo "→ Formatting TypeScript..."
    cd "$ROOT/apps/web"
    npm run format
    echo "✓ Format complete"
    ;;

  build)
    echo "→ Building backend..."
    cd "$ROOT/services/control-plane"
    ./gradlew build -x test
    echo "→ Building frontend..."
    cd "$ROOT/apps/web"
    npm run build
    echo "✓ Build complete"
    ;;

  health)
    echo "→ Checking service health..."
    curl -sf http://localhost:8080/api/health | python3 -m json.tool 2>/dev/null || \
      echo "  Backend: not reachable"
    curl -sf http://localhost:5678/healthz && echo "  n8n: UP" || echo "  n8n: not reachable"
    ;;

  *)
    usage
    exit 1
    ;;
esac
