#!/usr/bin/env bash
# Varredura estática por padrões de alto risco em arquivos do workspace.
# NÃO imprime o conteúdo dos matches (evita ecoar secrets). Exit 1 se encontrar.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail=0
hits_file="$(mktemp)"
trap 'rm -f "$hits_file"' EXIT

patterns=(
  'AKIA[0-9A-Z]{16}'
  'ASIA[0-9A-Z]{16}'
  '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----'
  'ghp_[A-Za-z0-9]{36}'
  'github_pat_[A-Za-z0-9_]{20,}'
  'xox[baprs]-[A-Za-z0-9-]{10,}'
  'sk-live-[A-Za-z0-9]{20,}'
  'sk-proj-[A-Za-z0-9]{20,}'
)

# Exclui fixtures de teste (marcadores PEM/PRIVATE KEY propositais no detector)
mapfile -t files < <(find . -type f \
  ! -path './.git/*' \
  ! -path '*/node_modules/*' \
  ! -path '*/.next/*' \
  ! -path '*/target/*' \
  ! -path '*/test-results/*' \
  ! -path '*/playwright-report/*' \
  ! -path '*/src/test/*' \
  ! -path '*/e2e/*' \
  ! -name 'package-lock.json' \
  ! -name '*.class' \
  ! -name '*.jar' \
  ! -name '*.png' \
  ! -name '*.jpg')

if [[ ${#files[@]} -eq 0 ]]; then
  echo "Nenhum arquivo para varrer (ok)."
  exit 0
fi

for pattern in "${patterns[@]}"; do
  : >"$hits_file"
  if grep -RInE --binary-files=without-match -e "$pattern" -- "${files[@]}" >"$hits_file" 2>/dev/null; then
    count=$(wc -l < "$hits_file" | tr -d ' ')
    echo "ALERTA: padrão sensível detectado ($pattern) em $count ocorrência(s)." >&2
    cut -d: -f1,2 "$hits_file" | sort -u | head -50 >&2
    fail=1
  fi
done

# Valor fictício do sample não deve aparecer em JSON fora de samples/
: >"$hits_file"
if grep -RInF --binary-files=without-match --include='*.json' \
  --exclude-dir=samples --exclude-dir=node_modules --exclude-dir=.next --exclude-dir=target \
  'demo-not-a-real-secret-123' . >"$hits_file" 2>/dev/null; then
  echo "ALERTA: valor fictício de demo encontrado em JSON fora de samples/" >&2
  cut -d: -f1,2 "$hits_file" | sort -u >&2
  fail=1
fi

if [[ "$fail" -ne 0 ]]; then
  echo "scan-secrets: FALHOU" >&2
  exit 1
fi

echo "scan-secrets: OK (nenhum padrão de alto risco encontrado)"
