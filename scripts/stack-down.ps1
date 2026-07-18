# Para a stack ArchLens preservando volumes (dados do Postgres permanecem).
# Uso (PowerShell): .\scripts\stack-down.ps1
$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)

$systemSock = "unix:///var/run/docker.sock"
$desktopSockPath = Join-Path $HOME ".docker/desktop/docker.sock"

if (-not $env:DOCKER_HOST) {
  $systemOk = (Test-Path "/var/run/docker.sock")
  if ($systemOk) {
    $prev = $env:DOCKER_HOST
    $env:DOCKER_HOST = $systemSock
    docker info 1>$null 2>$null
    $sysInfo = ($LASTEXITCODE -eq 0)
    if ($sysInfo) {
      $desktopOk = $false
      if (Test-Path $desktopSockPath) {
        $env:DOCKER_HOST = ("unix://" + $desktopSockPath)
        docker info 1>$null 2>$null
        $desktopOk = ($LASTEXITCODE -eq 0)
      }
      $env:DOCKER_HOST = $systemSock
      $names = docker ps --filter status=running --format '{{.Names}}' 2>$null
      $hasStack = [bool]($names | Select-String -Pattern '^(infra|archlens)-(keycloak|frontend|backend|postgres)(-\d+)?$')
      $ctx = ""
      if ($null -eq $prev) { Remove-Item Env:DOCKER_HOST -ErrorAction SilentlyContinue } else { $env:DOCKER_HOST = $prev }
      try { $ctx = (docker context show 2>$null) } catch { $ctx = "" }
      if (-not $desktopOk -or $hasStack -or $ctx -eq "desktop-linux") {
        $env:DOCKER_HOST = $systemSock
      }
    } else {
      if ($null -eq $prev) { Remove-Item Env:DOCKER_HOST -ErrorAction SilentlyContinue } else { $env:DOCKER_HOST = $prev }
    }
  }
}

# `stop` / `down` sem -v: containers param; volume é preservado.
docker compose -f infra/docker-compose.yml down
Write-Host "Stack parada. Volume de dados preservado (pgdata / infra_pgdata)."
Write-Host "Para apagar dados explicitamente: docker compose -f infra/docker-compose.yml down -v"
