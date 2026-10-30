#!/usr/bin/env bash
# E2E de autenticação OIDC real: Keycloak do Compose, backend com OIDC ligado (overlay
# infra/docker-compose.oidc.yml) e frontend apontando para esse backend.
#
# Prova: login Authorization Code + PKCE, API 401 sem token e com headers forjados, renovação do
# access token depois de expirar e falha fechada após revogação da sessão no IdP.
#
# Variáveis (todas com padrão para a stack do Compose):
#   ARCHLENS_E2E_BASE_URL   frontend (padrão http://127.0.0.1:3000; com PLAYWRIGHT_SKIP_WEBSERVER=1 usa um já em execução)
#   ARCHLENS_E2E_API_URL    endpoint protegido (padrão http://127.0.0.1:8080/api/v1/assessments)
#   ARCHLENS_E2E_KC_URL     Keycloak visto pelo host (padrão http://127.0.0.1:8081)
#   ARCHLENS_E2E_KC_CONTAINER  contêiner do Keycloak para o kcadm (padrão infra-keycloak-1)
#   ARCHLENS_E2E_TOKEN_SECONDS  vida do access token no realm durante o teste (padrão 40)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BASE_URL="${ARCHLENS_E2E_BASE_URL:-http://127.0.0.1:3000}"
API_URL="${ARCHLENS_E2E_API_URL:-http://127.0.0.1:8080/api/v1/assessments}"
KC_URL="${ARCHLENS_E2E_KC_URL:-http://127.0.0.1:8081}"
KC_CONTAINER="${ARCHLENS_E2E_KC_CONTAINER:-infra-keycloak-1}"
TOKEN_SECONDS="${ARCHLENS_E2E_TOKEN_SECONDS:-40}"
KCADM="docker exec $KC_CONTAINER /opt/keycloak/bin/kcadm.sh"

for i in $(seq 1 60); do
  curl -fsS -o /dev/null "$KC_URL/realms/archlens/.well-known/openid-configuration" && break
  [ "$i" -eq 60 ] && { echo "Keycloak não respondeu em $KC_URL" >&2; exit 1; }
  sleep 5
done

# Access token curto para provar a renovação dentro do tempo do teste; o realm importado tem 5 min.
$KCADM config credentials --server http://localhost:8081 --realm master --user admin --password admin >/dev/null
$KCADM update realms/archlens -s "accessTokenLifespan=$TOKEN_SECONDS"
echo "accessTokenLifespan=$TOKEN_SECONDS s no realm archlens"

cd frontend
ARCHLENS_E2E_REAL=1 ARCHLENS_E2E_OIDC=1 \
PLAYWRIGHT_BASE_URL="$BASE_URL" \
OIDC_E2E_USERNAME="${OIDC_E2E_USERNAME:-architect}" OIDC_E2E_PASSWORD="${OIDC_E2E_PASSWORD:-architect123}" \
OIDC_E2E_API_URL="$API_URL" OIDC_E2E_KC_ADMIN_URL="$KC_URL" OIDC_E2E_ACCESS_TOKEN_SECONDS="$TOKEN_SECONDS" \
npx playwright test e2e/oidc.spec.ts --retries=0 "$@"
