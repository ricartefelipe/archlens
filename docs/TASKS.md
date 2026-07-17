# ArchLens — Tasks (estado persistente)

Atualizado em 2026-07-17 (Prompt 2/5 — backend/persistência/regras/relatórios **concluído**).

## Diagnóstico Prompt 2 (antes das alterações)

- Analyzer Prompt 1 OK: schema, CLI, `/tmp/archlens-demo-analysis.json`, testes do reactor analyzer verdes.
- Backend existia, porém com lacunas críticas:
  - Testes usavam **H2**, não PostgreSQL real (apesar de dependência Testcontainers).
  - Isolamento org frágil: `findById` em snapshots/opções/roadmap; inventário sem `organization_id`.
  - Import: sem limite de payload, sem 409 em chave+payload diferente, sem gating de status.
  - `ReportService.renderPdf` engolia falhas com `byte[0]`; HTML incompleto.
  - `@Lob` mapeava para OID no PostgreSQL → falha em auto-commit.
  - Questionário/roadmap sem validação de faixa/fase; Problem JSON sem `correlationId`.

## Plano Prompt 2 — executado

1. [x] PostgreSQL real via Testcontainers (`PostgresTestResource`) + Liquibase migrate-at-start
2. [x] Migration `002` (índices compostos + `organization_id` no inventário)
3. [x] Isolamento org em consultas Panache + header `X-Organization-Id` + testes negativos 404
4. [x] Import: schema, max-bytes, idempotência/conflito 409, status, merge de findings, fixture real
5. [x] Motor de regras: IDs únicos no startup; evidências; ≥1 match + determinismo
6. [x] Questionário/scores com faixa 1–5, rationale e disclaimer
7. [x] Modernização/roadmap (effort/impact/phase) + refs na mesma org/assessment
8. [x] Relatório HTML/PDF válidos, snapshot imutável (409 na 2ª publicação)
9. [x] `application/problem+json` + `correlationId`
10. [x] Suíte IT PostgreSQL verde (20 testes)

## 0. Fundação

- [x] Inspeção do repositório (vazio)
- [x] `IMPLEMENTATION_PLAN.md` / `TASKS.md` / `DECISIONS.md`
- [x] Estrutura monorepo + README raiz
- [x] `contracts/analysis.schema.json`
- [x] Sample `legacy-java-demo`

## 1. Analyzer (Prompt 1)

- [x] Core + CLI + schema sync + docs + quality gate

## 2. Backend (Prompt 2)

- [x] Liquibase YAML PostgreSQL + migration corretiva 002
- [x] Import analysis real + inventário atômico + idempotência
- [x] ≥25 regras YAML + engine
- [x] Questionário versionado + dimensões
- [x] Achados (status) + revisão + auditoria
- [x] Opções modernização + roadmap
- [x] Relatório HTML/PDF + snapshot imutável
- [x] Isolamento multi-org (header + consultas tenant-scoped)
- [x] NarrativeGenerator template (LLM off por padrão)
- [x] Testes PostgreSQL: `ArchLensApiIT` + `OrgIsolationIT` + `LiquibaseMigrationIT` + `RuleEngineTest` — **20/20**

## 3. Frontend

- [x] Jornada E2E básica (Prompt anterior)
- [ ] Prompt 3+: consumir explicitamente `detector`/`confidence`/`durationMs` se UI exigir

## 4. Infra / CI / Docs

- [x] docker-compose + Keycloak + CI
- [ ] E2E Playwright/Compose completo no host
- [x] Threat model, ADRs, runbook, architecture

## Pendências para o próximo prompt

- UI alinhada a campos novos do analysis.json.
- Cobertura E2E Compose + Playwright.
- Endurecer OIDC multi-tenant além do header local `X-Organization-Id`.
- Política de retenção/compactação de `raw_json` em produção.
