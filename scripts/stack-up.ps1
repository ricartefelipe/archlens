# Sobe a stack ArchLens (demo local) preservando volumes.
# Uso (PowerShell): .\scripts\stack-up.ps1
$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)

if (-not $env:DOCKER_HOST) {
  # Docker Desktop no Windows normalmente usa o named pipe padrão.
  Write-Host "DOCKER_HOST não definido; usando Docker Desktop padrão."
}

docker compose -f infra/docker-compose.yml up --build -d
docker compose -f infra/docker-compose.yml ps
Write-Host ""
Write-Host "Frontend:  http://localhost:3000"
Write-Host "API ready: http://localhost:8080/q/health/ready"
Write-Host "Metrics:   http://localhost:8080/q/metrics"
Write-Host "Keycloak:  http://localhost:8081"
Write-Host ""
Write-Host "Para parar SEM apagar dados: .\scripts\stack-down.ps1"
Write-Host "NÃO use 'docker compose down -v' a menos que queira apagar o volume pgdata."
