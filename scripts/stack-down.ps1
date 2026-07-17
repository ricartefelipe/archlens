# Para a stack ArchLens preservando volumes (dados do Postgres permanecem).
# Uso (PowerShell): .\scripts\stack-down.ps1
$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)

# `stop` / `down` sem -v: containers param; volume archlens_pgdata é preservado.
docker compose -f infra/docker-compose.yml down
Write-Host "Stack parada. Volume de dados preservado (pgdata / infra_pgdata)."
Write-Host "Para apagar dados explicitamente: docker compose -f infra/docker-compose.yml down -v"
