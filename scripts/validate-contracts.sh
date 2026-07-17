#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

test -f contracts/analysis.schema.json
test -f contracts/openapi/archlens-api.yaml

# Schema mínimo: JSON válido com propriedades obrigatórias do contrato
python3 - <<'PY'
import json
from pathlib import Path
schema = json.loads(Path("contracts/analysis.schema.json").read_text(encoding="utf-8"))
assert schema.get("$schema") or schema.get("type") == "object" or "properties" in schema
required = set(schema.get("required", []))
for key in ("schemaVersion", "analyzerVersion", "project", "contentHash"):
    assert key in schema.get("properties", {}) or key in required or key in schema.get("properties", {}), f"missing {key}"
print("analysis.schema.json: OK")
PY

# OpenAPI presente com path base
grep -q 'openapi:' contracts/openapi/archlens-api.yaml
grep -Eq '/api/v1' contracts/openapi/archlens-api.yaml

# Backend embute o mesmo schema
test -f backend/src/main/resources/analysis.schema.json
cmp -s contracts/analysis.schema.json backend/src/main/resources/analysis.schema.json \
  || { echo "Schema contracts/ e backend/ divergem" >&2; exit 1; }

echo "contracts: OK"
