# ArchLens — Tasks (estado persistente)

Atualizado em 2026-07-17 (Prompt 4/5 — hardening/infra/CI/docs).

## Diagnóstico Prompt 4 (baseline)

Riscos encontrados antes das alterações:

| Severidade | Risco |
|------------|--------|
| Alta | Compose usava `QUARKUS_PROFILE=prod` com OIDC off e header auth (falso prod) |
| Alta | TLS OIDC `none` e secrets com defaults mesmo em “prod” |
| Alta | Keycloak `webOrigins: "+"` permissivo |
| Média | Sem `.dockerignore`, containers root, CI incompleta |
| Média | Sem métricas/Prometheus, rate limit, headers de segurança consistentes |
| Média | Docs/runbook sem backup, perfis nem comandos Windows de preserve-data |
| Baixa | Swagger potencialmente público em pacote “prod” |

## Plano Prompt 4 — executado

1. [x] Perfis `dev` / `test` / `demo` / `prod` + `SecurityConfigValidator`
2. [x] IdentityService: header auth só com `archlens.local-header-auth.enabled`
3. [x] Keycloak realm: clients separados, webOrigins restritos, PKCE, usuários fictícios
4. [x] Compose demo + overlay prod exemplo; healthchecks; volumes nomeados; non-root
5. [x] Métricas, correlation MDC, security headers, rate limit import, readiness regras
6. [x] CI expandida (analyzer, secret scan, backend IT, liquibase pack, frontend gates, e2e, compose, contracts)
7. [x] Docs: README, RUNBOOK, ARCHITECTURE, THREAT_MODEL, ENVIRONMENTS, OBSERVABILITY, DECISIONS, TASKS
8. [x] Quality gate runtime (compose up/health/jornada/restart) — validado nesta sessão

## 0–3. Fundação / Analyzer / Backend / Frontend

- Prompts 1–3 concluídos (ver histórico).

## 4. Infra / CI / Docs (este prompt)

- [x] Separação demo vs produção
- [x] Endurecimento containers/CI/observabilidade
- [x] Quality gate local: contracts, secret-scan, backend tests (20), compose health/ready, métricas, headers, jornada API + RBAC VIEWER 403, analyzer sem vazamento, restart preservando assessment, fail-fast `%prod`, frontend lint/typecheck/test
- [ ] OIDC Bearer E2E completo no Compose (ainda opcional; backend demo usa headers)
- [ ] Prompt 5 — validação final MVP (não declarar produção)

**Não declarar MVP pronto para produção.**

## Classificação de capacidades

| Capacidade | Estado |
|------------|--------|
| Analyzer + schema + fixture | Implementada e testada |
| Backend jornada + IT Postgres | Implementada e testada |
| Frontend + E2E real | Implementada e testada (Prompt 3) |
| Hardening perfil prod | Implementada; fail-fast testado via config |
| OIDC ponta a ponta Bearer | Implementada não totalmente testada |
| Métricas/dashboards | Implementada (endpoint); dashboards externos planejados |
| Gradle / sinais | Heurística |
| LLM | Opcional |
| Multi-tenant / K8s | Planejada (fora de escopo) |

## Pendências para Prompt 5

- Validação final end-to-end e checklist de “não pronto para produção”.
- Decidir se demo Compose passa a oferecer perfil opcional com OIDC no backend.
- Filtros de findings no servidor; retenção/UI de `raw_json`.
