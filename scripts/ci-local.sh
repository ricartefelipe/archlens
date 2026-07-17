#!/usr/bin/env bash
# Equivalente local aos quality gates da CI (sem deploy, sem git).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export DOCKER_HOST="${DOCKER_HOST:-unix:///var/run/docker.sock}"

echo "==> contracts"
bash scripts/validate-contracts.sh

echo "==> secret-scan"
bash scripts/scan-secrets.sh

echo "==> compose config"
docker compose -f infra/docker-compose.yml config --quiet

echo "==> analyzer"
(
  cd analyzer
  ./mvnw -B test
  ./mvnw -B -pl cli -am package -DskipTests
  java -jar cli/target/archlens-analyzer-cli-*.jar scan \
    --path ../samples/legacy-java-demo --out /tmp/analysis.json
  java -jar cli/target/archlens-analyzer-cli-*.jar validate --file /tmp/analysis.json
  if grep -F 'demo-not-a-real-secret-123' /tmp/analysis.json; then
    echo "Segredo fictício vazou" >&2
    exit 1
  fi
)

echo "==> backend tests"
(
  cd backend
  ./mvnw -B test
)

echo "==> frontend"
(
  cd frontend
  npm ci
  npm run lint
  npm run typecheck
  npm run test
  npm run build
)

echo "ci-local: OK"
