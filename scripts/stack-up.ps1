# Sobe a stack ArchLens (demo local) preservando volumes.
# Uso (PowerShell): .\scripts\stack-up.ps1
$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)

$systemSock = "unix:///var/run/docker.sock"
$desktopSockPath = Join-Path $HOME ".docker/desktop/docker.sock"

function Test-DockerHost([string]$HostUri) {
  $prev = $env:DOCKER_HOST
  try {
    $env:DOCKER_HOST = $HostUri
    docker info 1>$null 2>$null
    return ($LASTEXITCODE -eq 0)
  } finally {
    if ($null -eq $prev) { Remove-Item Env:DOCKER_HOST -ErrorAction SilentlyContinue }
    else { $env:DOCKER_HOST = $prev }
  }
}

function Test-SystemRunningStack {
  $prev = $env:DOCKER_HOST
  try {
    $env:DOCKER_HOST = $systemSock
    $names = docker ps --filter status=running --format '{{.Names}}' 2>$null
    if ($LASTEXITCODE -ne 0) { return $false }
    return [bool]($names | Select-String -Pattern '^(infra|archlens)-(keycloak|frontend|backend|postgres)(-\d+)?$')
  } finally {
    if ($null -eq $prev) { Remove-Item Env:DOCKER_HOST -ErrorAction SilentlyContinue }
    else { $env:DOCKER_HOST = $prev }
  }
}

if (-not $env:DOCKER_HOST) {
  $systemOk = (Test-Path "/var/run/docker.sock") -and (Test-DockerHost $systemSock)
  $desktopOk = (Test-Path $desktopSockPath) -and (Test-DockerHost ("unix://" + $desktopSockPath))

  if ($systemOk -and -not $desktopOk) {
    $env:DOCKER_HOST = $systemSock
    Write-Host "Docker Desktop inativo; usando DOCKER_HOST=$env:DOCKER_HOST"
  } elseif ($systemOk -and (Test-SystemRunningStack)) {
    $env:DOCKER_HOST = $systemSock
    Write-Host "Stack ArchLens já no Engine do sistema; usando DOCKER_HOST=$env:DOCKER_HOST"
  } elseif ($systemOk) {
    $ctx = ""
    try { $ctx = (docker context show 2>$null) } catch { $ctx = "" }
    if ($ctx -eq "desktop-linux") {
      $env:DOCKER_HOST = $systemSock
      Write-Host "Contexto Desktop ativo, mas preferindo Engine do sistema (DOCKER_HOST=$env:DOCKER_HOST)"
      Write-Host "Para forçar Desktop: `$env:DOCKER_HOST='unix://$desktopSockPath'; .\scripts\stack-up.ps1"
    } else {
      Write-Host "DOCKER_HOST não definido; usando contexto Docker atual."
    }
  } else {
    Write-Host "DOCKER_HOST não definido; usando Docker Desktop padrão."
  }
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
