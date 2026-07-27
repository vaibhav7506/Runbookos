@echo off
REM RunbookOS development scripts (Windows PowerShell wrapper)
REM For full functionality, use Git Bash or WSL to run scripts\dev.sh

setlocal enabledelayedexpansion

set "ROOT=%~dp0.."
set "CMD=%1"

if "%CMD%"=="infra" goto :infra
if "%CMD%"=="infra:down" goto :infra_down
if "%CMD%"=="all" goto :all
if "%CMD%"=="all:down" goto :all_down
if "%CMD%"=="backend" goto :backend
if "%CMD%"=="frontend" goto :frontend
if "%CMD%"=="test:backend" goto :test_backend
if "%CMD%"=="test:frontend" goto :test_frontend
if "%CMD%"=="health" goto :health
goto :usage

:infra
echo Starting infrastructure services...
docker compose -f "%ROOT%\docker-compose.yml" up -d postgres redis n8n
goto :eof

:infra_down
echo Stopping infrastructure services...
docker compose -f "%ROOT%\docker-compose.yml" stop postgres redis n8n
goto :eof

:all
echo Starting all services...
docker compose -f "%ROOT%\docker-compose.yml" up
goto :eof

:all_down
echo Stopping all services...
docker compose -f "%ROOT%\docker-compose.yml" down
goto :eof

:backend
echo Starting Spring Boot control plane...
cd "%ROOT%\services\control-plane"
call gradlew.bat bootRun
goto :eof

:frontend
echo Starting Next.js web application...
cd "%ROOT%\apps\web"
call npm run dev
goto :eof

:test_backend
echo Running backend tests...
cd "%ROOT%\services\control-plane"
call gradlew.bat test
goto :eof

:test_frontend
echo Running frontend tests and checks...
cd "%ROOT%\apps\web"
call npm run lint
call npm run type-check
call npm test
goto :eof

:health
echo Checking service health...
curl -sf http://localhost:8080/api/health
goto :eof

:usage
echo Usage: scripts\dev.bat ^<command^>
echo.
echo Commands:
echo   infra        Start infrastructure (postgres, redis, n8n)
echo   infra:down   Stop infrastructure
echo   backend      Start Spring Boot control plane
echo   frontend     Start Next.js web application
echo   all          Start all services via Docker Compose
echo   all:down     Stop all Docker Compose services
echo   test:backend Run backend tests
echo   test:frontend Run frontend checks
echo   health       Check health endpoints
echo.
goto :eof
