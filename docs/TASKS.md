# ArchLens — Tasks (estado persistente)

Atualizado em 2026-07-17 (Prompt 1/5 — analyzer offline **concluído**).

## Diagnóstico Prompt 1 (antes das alterações)

- Projeto confirmado: reactor `analyzer/` (Java 21, módulos `core` + `cli`) + monorepo ArchLens.
- Build/testes do reactor `analyzer` **passavam**, porém com implementação incompleta frente ao contrato de segurança:
  - `MavenPomParser` / `GradleBuildParser`: regex frágil; `dependencyManagement` misturado com deps; sem XML seguro / XXE.
  - `SafeFileWalker`: sem limites, sem bloqueio explícito de symlink externo, sem ignore de binários/`.vscode`.
  - `SecretDetector`: `matcher.group()` alimentava evidência; hash SHA-256 direto do valor (correlacionável).
  - Sinais MVP incompletos (PrimeFaces, Flyway, OAuth/OIDC, OpenAPI, health/metrics, RMI, etc.).
  - CLI: códigos de saída não distinguiam entrada inválida vs falha de schema vs erro interno.
  - Docs de contrato/limitações ausentes (`ANALYSIS_CONTRACT.md`, `LIMITATIONS.md`).

## Plano Prompt 1 — executado

1. [x] MavenPomParser XML seguro (XXE/DTD off) + testes
2. [x] GradleBuildParser heurístico documentado + testes
3. [x] SafeFileWalker com limites/symlink/binários + testes
4. [x] SecretDetector / EvidenceFactory / indicador não correlacionável
5. [x] SignalDetector + inventário MVP
6. [x] Contrato schema + sync backend + determinismo + CLI exit codes
7. [x] Docs + quality gate (testes, package, scan, validate, hash, anti-leak)

## 0. Fundação

- [x] Inspeção do repositório (vazio)
- [x] `IMPLEMENTATION_PLAN.md` / `TASKS.md` / `DECISIONS.md`
- [x] Estrutura monorepo + README raiz
- [x] `contracts/analysis.schema.json`
- [x] Sample `legacy-java-demo`

## 1. Analyzer (Prompt 1)

- [x] Core: walk seguro, ignores, limites, parsers Maven/Gradle seguros
- [x] Proteção de segredos (sem valor, sem hash correlacionável simples)
- [x] Sinais/inventário MVP + evidências sem fonte
- [x] CLI: `scan`, `validate`, `version` com exit codes distintos
- [x] JSON determinístico + contentHash canônico
- [x] Schema sync contracts ↔ backend
- [x] Docs: `ANALYSIS_CONTRACT.md`, `LIMITATIONS.md`
- [x] Quality gate Prompt 1 (35 testes; scan demo validado)

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
- [ ] Prompt 2+: alinhar materialização a novos campos `detector`/`confidence`/`durationMs` se UI exigir

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

## Pendências para o próximo prompt

- Import backend/UI consumindo `detector`/`confidence`/`durationMs` de forma explícita (hoje ignora campos extras com `@JsonIgnoreProperties`).
- Cobertura E2E Compose + Playwright.
- Evolução de regras/roadmap a partir dos novos sinais (OAuth/OIDC, Flyway, etc.).
- Refinar inventário de descritores WAS/JBoss com fixtures dedicadas além do sample atual.
