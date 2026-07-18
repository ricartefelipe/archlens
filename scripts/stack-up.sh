#!/usr/bin/env bash
# Sobe a stack ArchLens (demo local) preservando volumes.
# Uso: ./scripts/stack-up.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

SYSTEM_SOCK="unix:///var/run/docker.sock"
DESKTOP_SOCK_PATH="${HOME}/.docker/desktop/docker.sock"

system_sock_usable() {
  [[ -S /var/run/docker.sock ]] && DOCKER_HOST="$SYSTEM_SOCK" docker info >/dev/null 2>&1
}

desktop_sock_usable() {
  [[ -S "$DESKTOP_SOCK_PATH" ]] && DOCKER_HOST="unix://${DESKTOP_SOCK_PATH}" docker info >/dev/null 2>&1
}

system_has_running_stack() {
  DOCKER_HOST="$SYSTEM_SOCK" docker ps --filter status=running --format '{{.Names}}' 2>/dev/null \
    | grep -qE '^(infra|archlens)-(keycloak|frontend|backend|postgres)(-[0-9]+)?$'
}

resolve_docker_host() {
  if [[ -n "${DOCKER_HOST:-}" ]]; then
    echo "Usando DOCKER_HOST já definido: $DOCKER_HOST"
    return
  fi

  # Desktop sock ausente/inativo → Engine do sistema.
  if system_sock_usable && ! desktop_sock_usable; then
    export DOCKER_HOST="$SYSTEM_SOCK"
    echo "Docker Desktop inativo; usando DOCKER_HOST=$DOCKER_HOST"
    return
  fi

  # Stack já healthy/running no sistema → não tentar segunda cópia no Desktop.
  if system_sock_usable && system_has_running_stack; then
    export DOCKER_HOST="$SYSTEM_SOCK"
    echo "Stack ArchLens já no Engine do sistema; usando DOCKER_HOST=$DOCKER_HOST"
    return
  fi

  # Linux com sock do sistema disponível: preferir default/context, mas avisar se Desktop.
  if system_sock_usable; then
    local ctx
    ctx="$(docker context show 2>/dev/null || true)"
    if [[ "$ctx" == "desktop-linux" ]]; then
      export DOCKER_HOST="$SYSTEM_SOCK"
      echo "Contexto Desktop ativo, mas preferindo Engine do sistema (DOCKER_HOST=$DOCKER_HOST)"
      echo "Para forçar Desktop: DOCKER_HOST=unix://${DESKTOP_SOCK_PATH} $0"
      return
    fi
  fi

  echo "DOCKER_HOST não definido; usando contexto Docker atual ($(docker context show 2>/dev/null || echo default))."
}

resolve_docker_host

docker compose -f infra/docker-compose.yml up --build -d
docker compose -f infra/docker-compose.yml ps

echo ""
echo "Frontend:  http://localhost:3000"
echo "API ready: http://localhost:8080/q/health/ready"
echo "Metrics:   http://localhost:8080/q/metrics"
echo "Keycloak:  http://localhost:8081"
echo ""
echo "Para parar SEM apagar dados: ./scripts/stack-down.sh"
echo "NÃO use 'docker compose down -v' a menos que queira apagar o volume pgdata."
