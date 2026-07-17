# ArchLens — Tasks (estado persistente)

Atualizado em **2026-07-17** (Prompt 5/5 — validação final / MVP demonstrável).

## Prompt 5 — executado

1. [x] Auditoria de escopo (analyzer, schema, fixture, backend, frontend, Compose, CI, docs)
2. [x] Gates do zero: contracts, secret-scan, analyzer, CLI+fixture, backend IT, frontend lint/typecheck/test/build, E2E, restart/persistência
3. [x] Correção residual: `ARCHLENS_API_INTERNAL_URL` para server actions no Compose
4. [x] `docs/MVP_READINESS.md`, `docs/DEMO_SCRIPT.md`, `docs/COMMERCIAL_READINESS.md`
5. [x] Atualização LIMITATIONS / ENVIRONMENTS / TASKS

**Veredito:** `MVP DEMONSTRÁVEL` (jornada principal verde). **Não** pronto para produção.

## 0–4. Histórico

- Prompts 1–3: analyzer, backend, frontend/E2E.
- Prompt 4: hardening demo vs prod, CI, observabilidade, docs.

## Pendências pós-MVP (não bloqueiam demo)

- [ ] OIDC Bearer E2E completo como caminho Compose opcional (hoje demo = headers + login local)
- [ ] Filtros de findings no servidor
- [ ] Retenção/UI explícita de `raw_json`
- [ ] Overlay prod com secrets reais em ambiente de cliente (piloto)
- [ ] Dashboards externos (Prometheus já exposto)

## Classificação resumida

Ver matriz completa em [MVP_READINESS.md](./MVP_READINESS.md).

| Capacidade | Estado |
|------------|--------|
| Analyzer + schema + fixture | IMPLEMENTED_AND_TESTED |
| Backend jornada + IT Postgres | IMPLEMENTED_AND_TESTED |
| Frontend + E2E real (Compose) | IMPLEMENTED_AND_TESTED |
| Persistência volume Compose | IMPLEMENTED_AND_TESTED |
| Hardening perfil prod | IMPLEMENTED_AND_TESTED (fail-fast) |
| OIDC Bearer ponta a ponta | IMPLEMENTED_NOT_FULLY_TESTED |
| Gradle / sinais | HEURISTIC |
| LLM | OPTIONAL_EXTERNAL_INTEGRATION |
| Multi-tenant SaaS / K8s | PLANNED / BLOCKED para SaaS público |

**Não declarar MVP pronto para produção.**
