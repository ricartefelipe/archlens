# Observabilidade — ArchLens

## Correlation ID

- Header `X-Correlation-Id` aceito e propagado na resposta.
- Gravado em MDC (`correlationId`) para logs.
- Incluído em Problem Details (`correlationId`).
- Frontend gera/propaga o mesmo header nas chamadas API.

## Logs

| Ambiente | Formato |
|----------|---------|
| demo / dev / test | texto com `correlationId` |
| produção (`%prod`) | JSON estruturado (`quarkus-logging-json`) |

**Proibido em logs:** tokens, client secrets, senhas, corpo completo de `analysis.json`, código-fonte.

## Métricas (Prometheus `/q/metrics`)

| Métrica | Descrição |
|---------|-----------|
| `archlens_import_total{result=success}` | Importações bem-sucedidas |
| `archlens_import_schema_failure_total` | Falhas de schema/parse |
| `archlens_findings_generated_total` | Achados materializados no import |
| `archlens_import_duration_seconds` | Duração do import |
| `archlens_report_generated_total` | Relatórios gerados |
| `archlens_report_duration_seconds` | Duração da geração |
| `archlens_report_pdf_failure_total` | Falhas de PDF |
| `archlens_oidc_failure_total` | Reservado para falhas OIDC |
| `archlens_db_failure_total` | Reservado para falhas de DB |

Duração do analyzer é medida no CLI (offline); o backend registra duração de **importação** do JSON.

## Health vs readiness

- **Liveness** (`/q/health/live`): processo JVM respondendo.
- **Readiness** (`/q/health/ready`): datasource + check de regras carregadas.
- Compose `depends_on` usa readiness do backend.

## Tracing

OpenTelemetry **opcional / planejado** — não embutido como dependência obrigatória do MVP. Quando necessário, adicionar extensão Quarkus OTel e coletor externo.

## Dashboards e alertas mínimos (sem inventar infra)

Sugestão ao conectar um Prometheus/Grafana já existente:

1. **Alerta:** `rate(archlens_import_schema_failure_total[5m]) > 0` por vários minutos.
2. **Alerta:** `rate(archlens_report_pdf_failure_total[15m]) > 0`.
3. **Painel:** taxa de imports, p95 `archlens_import_duration_seconds`, contagem de findings.
4. **Painel:** readiness do backend (probe HTTP `/q/health/ready`).

Não há stack de métricas embutida no Compose além do endpoint Prometheus do Quarkus.
