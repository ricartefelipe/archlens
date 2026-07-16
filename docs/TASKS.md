# ArchLens — Tasks (estado persistente)

Atualizado em 2026-07-16.

## 0. Fundação

- [x] Inspeção do repositório (vazio)
- [x] `IMPLEMENTATION_PLAN.md` / `TASKS.md` / `DECISIONS.md`
- [x] Estrutura monorepo + README raiz
- [x] `contracts/analysis.schema.json`
- [x] Sample `legacy-java-demo`

## 1. Analyzer

- [x] Core: walk seguro, ignores, detecção de stack/sinais
- [x] CLI: `scan`, `validate`, `version`
- [x] JSON determinístico + SHA-256
- [x] Testes: fixture, schema, determinismo

## 2. Backend

- [x] Módulos domain/application/infrastructure/api/report
- [x] Liquibase YAML + entidades Panache (não expostas)
- [x] Import analysis + idempotency + materialização
- [x] ≥25 regras YAML + engine (27 regras)
- [x] Questionário versionado + dimensões
- [x] Achados (status) + revisão + auditoria
- [x] Opções modernização + roadmap
- [x] Relatório HTML/PDF + snapshot imutável
- [x] OIDC configurável + papéis via headers em local
- [x] NarrativeGenerator (template default)
- [x] Testes API (`ArchLensApiIT` + `RuleEngineTest`) — 10/10

## 3. Frontend

- [x] Next.js App Router + auth (local credentials + Keycloak opcional)
- [x] Páginas da jornada E2E
- [x] Mermaid + fallback + CSV export
- [x] a11y básica
- [x] Cliente API alinhado ao backend

## 4. Infra / CI / Docs

- [x] docker-compose + healthchecks + Keycloak realm
- [x] GitHub Actions CI
- [x] Threat model, ADRs, runbook, architecture
- [ ] E2E Playwright completo (smoke local; Compose não validado neste host — Docker daemon indisponível)
- [x] Quality gates principais validados via Maven/npm/CLI

## Pendências conhecidas

- `docker compose up --build` não executado aqui: Docker daemon inacessível (`docker.sock`).
- Playwright E2E end-to-end contra stack completa pendente de ambiente com Docker.
- OIDC “duro” no compose local está desligado (`ARCHLENS_OIDC_ENABLED=false`); Keycloak sobe para evolução/login UI opcional.
