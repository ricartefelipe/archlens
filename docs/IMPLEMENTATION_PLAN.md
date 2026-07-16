# ArchLens — Plano de Implementação (MVP)

## Objetivo

Entregar plataforma local de diagnóstico arquitetural e modernização de apps Java legadas, sem enviar código-fonte para serviços externos.

## Escopo MVP

Jornada E2E: Auth → avaliação → CLI scan → import `analysis.json` → inventário → regras (≥25) → questionário → dimensões → revisão de achados → opções/roadmap → relatório HTML/PDF (snapshot) → auditoria.

## Arquitetura

Monólito modular:

| Módulo | Responsabilidade |
|--------|------------------|
| `contracts/` | JSON Schema + OpenAPI |
| `analyzer/` | Análise estática somente-leitura (core + CLI Picocli) |
| `backend/` | Quarkus 3.33 LTS — domínio, aplicação, infra, API, relatório |
| `frontend/` | Next.js App Router + OIDC |
| `infra/` | Docker Compose, Keycloak, Postgres |
| `samples/legacy-java-demo/` | Fixture com sinais legados |

## Fatias verticais

1. Contratos + sample + CLI scan/validate
2. Importação idempotente + materialização inventário
3. Motor de regras + achados
4. Questionário + dimensões + scores
5. Auth/RBAC + APIs REST
6. Frontend jornada completa
7. Opções/roadmap + relatório + snapshot
8. Auditoria + Docker + CI + docs

## Premissas

- Uma organização por instalação (`organization_id` no modelo)
- Analyzer nunca executa código do alvo
- LLM opcional desligado por padrão (`LLM_ENABLED=false`)
- Timestamps UTC; IDs UUID

## Estimativas (faixas)

| Fatia | Esforço | Confiança | Premissas |
|-------|---------|-----------|-----------|
| Analyzer + sample | 1–2 dias | alta | Parsing textual/AST leve suficiente |
| Backend core + regras | 2–4 dias | média | Quarkus/Panache/Liquibase estáveis |
| Frontend E2E | 2–3 dias | média | Keycloak local estável |
| Relatório/PDF/CI | 1–2 dias | média | HTML→PDF local sem headless browser |

**Total MVP:** 6–11 dias-pessoa (faixa; não é certeza).

## Riscos

- PDF com layout complexo → mitigar com HTML estático + biblioteca Java
- Keycloak no Compose → realm import + healthchecks
- Determinismo do JSON → ordenação canônica + exclusão de campos voláteis
