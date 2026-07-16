# Motor de regras

## Formato YAML

```yaml
id: RULE_EJB_PRESENT
name: Uso de EJB detectado
severity: HIGH
dimension: technology_debt
condition:
  type: signal_present
  signal: EE_EJB
message: "O inventário indica EJB; planejar substituição por serviços CDI/Spring."
```

## Condições enumeradas (MVP)

- `signal_present`
- `metric_gt` / `metric_lt`
- `cycle_present`
- `secret_present`
- `hotspot_present`
- `dependency_matches`
- `framework_present`
- `endpoint_type_present`
- `test_ratio_lt`

## Achados

Status: `OPEN` → `APPROVED` | `EDITED` | `RISK_ACCEPTED` | `DISMISSED`.

Toda transição gera `AuditEvent`. Evidências referenciam IDs do `analysis.json` (sem código-fonte).

## Cobertura mínima

≥25 regras versionadas em `backend/src/main/resources/rules/`.
