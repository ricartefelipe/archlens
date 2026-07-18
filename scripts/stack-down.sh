#!/usr/bin/env bash
# Para a stack ArchLens preservando volumes.
# Uso: ./scripts/stack-down.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

SYSTEM_SOCK="unix:///var/run/docker.sock"
DESKTOP_SOCK_PATH="${HOME}/.docker/desktop/docker.sock"

if [[ -z "${DOCKER_HOST:-}" ]]; then
  if [[ -S /var/run/docker.sock ]] && DOCKER_HOST="$SYSTEM_SOCK" docker info >/dev/null 2>&1; then
    if ! [[ -S "$DESKTOP_SOCK_PATH" ]] || ! DOCKER_HOST="unix://${DESKTOP_SOCK_PATH}" docker info >/dev/null 2>&1; then
      export DOCKER_HOST="$SYSTEM_SOCK"
    elif DOCKER_HOST="$SYSTEM_SOCK" docker ps --filter status=running --format '{{.Names}}' 2>/dev/null \
      | grep -qE '^(infra|archlens)-(keycloak|frontend|backend|postgres)(-[0-9]+)?$'; then
      export DOCKER_HOST="$SYSTEM_SOCK"
    elif [[ "$(docker context show 2>/dev/null || true)" == "desktop-linux" ]]; then
      export DOCKER_HOST="$SYSTEM_SOCK"
    fi
  fi
fi

docker compose -f infra/docker-compose.yml down
echo "Stack parada. Volume de dados preservado (pgdata / infra_pgdata)."
echo "Para apagar dados explicitamente: docker compose -f infra/docker-compose.yml down -v"
