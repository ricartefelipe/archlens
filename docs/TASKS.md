# ArchLens — Tasks (estado persistente)

Atualizado em 2026-07-17 (Prompt 3/5 — frontend/auth/E2E **concluído**).

## Diagnóstico Prompt 3 (baseline antes das alterações)

- Projeto ArchLens confirmado (analyzer + backend + frontend + infra).
- Baseline frontend: `npm ci` OK; lint com 4 warnings (roles não usados); `tsc` OK; `build` OK.
- Lacunas:
  - Cliente API sem Problem Details / correlationId / timeout.
  - Login local sempre ativo (senhas no módulo auth); Keycloak em porta errada no `.env.local`.
  - Fallback silencioso `X-Roles=ADMIN` via `NEXT_PUBLIC_DEV_*`.
  - Roadmap enviava `phase` numérica; backend exige `SHORT_TERM|MEDIUM_TERM|LONG_TERM`.
  - Upload sem validação client-side; downloads dependiam de CORS frágil.
  - E2E apenas smoke; testes unitários ausentes.
  - Compose: OIDC desligado no backend (headers locais); Keycloak client público.

## Plano Prompt 3 — executado

1. [x] Cliente API: base URL, timeout, correlationId, `application/problem+json`, download com content-type/filename
2. [x] Auth: `AUTH_LOCAL_ENABLED`; Keycloak opcional; middleware; papéis na UI; 401/403 tratados
3. [x] Jornada UX: criação, import (validação), inventário, findings, questionário, scores, roadmap, relatório/publicação/download, auditoria
4. [x] Segurança UI: sem log de token/JSON; upload validado; downloads via server action; Mermaid strict
5. [x] Testes unitários (Vitest 12) + Playwright real (10)
6. [x] Quality gate: lint / typecheck / test / build / E2E
7. [x] CORS backend: `quarkus.http.cors.enabled=true`; client Keycloak confidential para NextAuth

## 0–2. Fundação / Analyzer / Backend

- Ver histórico Prompt 1 e 2 (concluídos).

## 3. Frontend (Prompt 3)

- [x] Rotas e componentes da jornada
- [x] Contratos alinhados (assessment `projectRootName`, roadmap phases, Problem Details)
- [x] A11y básica (labels, alertas, foco em controles)
- [x] E2E Compose/Playwright com serviços reais (Postgres + backend + frontend local)

## 4. Infra / CI / Docs

- [x] docker-compose com env de auth no frontend
- [x] E2E Playwright hospedado (stack parcial Compose + frontend `npm run dev`)
- [ ] Endurecer OIDC end-to-end (backend `ARCHLENS_OIDC_ENABLED=true` + token real)
- [x] Threat model / ADRs / runbook (pré-existentes)

## Pendências para o próximo prompt

- Ligar OIDC no backend no Compose integrado e validar Bearer (não só `X-User`/`X-Roles`).
- Filtros de findings no servidor (hoje filtragem client-side após GET completo).
- Política de retenção de `raw_json` e UI de modernização options dedicada.
- Dependência CORS do Quarkus: confirmar headers `Access-Control-*` após rebuild limpo (downloads críticos já vão por server action).
