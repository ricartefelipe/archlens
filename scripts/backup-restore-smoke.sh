#!/usr/bin/env bash
# Prova dump e restauração em um Postgres efêmero.
# Não usa volume, senha ou dado de cliente. Exit 2 se o Docker não estiver acessível.
set -euo pipefail

export DOCKER_HOST="${DOCKER_HOST:-unix:///var/run/docker.sock}"

if ! docker info >/dev/null 2>&1; then
  echo "BLOQUEIO: Docker indisponível em ${DOCKER_HOST}. Restauração não executada." >&2
  exit 2
fi

NAME="archlens-backup-smoke-$$"
docker run -d --name "$NAME" \
  -e POSTGRES_PASSWORD=archlens-smoke-only \
  -e POSTGRES_DB=archlens \
  postgres:16.6-alpine >/dev/null

cleanup() {
  docker rm -f "$NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

ready=0
for _ in $(seq 1 40); do
  if docker exec "$NAME" pg_isready -U postgres >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 1
done
if [[ "$ready" -ne 1 ]]; then
  echo "BLOQUEIO: Postgres efêmero não ficou pronto." >&2
  exit 2
fi

docker exec "$NAME" psql -U postgres -d archlens -v ON_ERROR_STOP=1 \
  -c "CREATE TABLE smoke(id int primary key); INSERT INTO smoke VALUES (1);" >/dev/null
docker exec "$NAME" pg_dump -U postgres -d archlens -Fc -f /tmp/archlens.dump
docker exec "$NAME" psql -U postgres -d archlens -v ON_ERROR_STOP=1 -c "DROP TABLE smoke;" >/dev/null
docker exec "$NAME" pg_restore -U postgres -d archlens --clean --if-exists /tmp/archlens.dump
count="$(docker exec "$NAME" psql -U postgres -d archlens -tAc "SELECT count(*) FROM smoke;")"
count="${count// /}"
if [[ "$count" != "1" ]]; then
  echo "backup-restore-smoke: FALHOU (count=${count})" >&2
  exit 1
fi
echo "backup-restore-smoke: OK (1 linha restaurada em Postgres efêmero)"
