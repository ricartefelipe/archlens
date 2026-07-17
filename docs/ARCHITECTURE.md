# Arquitetura — ArchLens

## Visão

```
┌─────────────┐     analysis.json      ┌──────────────────┐
│ Analyzer CLI│ ─────────────────────► │ Backend Quarkus  │
│ (local)     │                        │ domain/app/infra │
└─────────────┘                        └────────┬─────────┘
                                                │ REST + auth
┌─────────────┐                        ┌────────▼─────────┐
│ Sample demo │                        │ Frontend Next.js │
└─────────────┘                        └────────┬─────────┘
                                                │
                                       ┌────────▼─────────┐
                                       │ Keycloak + PG    │
                                       └──────────────────┘
```

## Limites reais do MVP

- **Monólito modular** — sem Kubernetes, Kafka ou microsserviços.
- **Uma organização por instalação.**
- **Analyzer offline** — sem rede; não executa build do alvo.
- **Compose demo** usa auth por headers no backend; OIDC endurecido existe no perfil `%prod` mas Bearer E2E completo ainda não é o caminho padrão do Compose.
- **LLM** opcional e desligado por padrão.
- **Não declarar pronto para produção** até validação final (Prompt 5).

## Fluxo de dados

1. Operador executa `archlens scan` no código legado (somente leitura).
2. CLI emite `analysis.json` validado pelo schema compartilhado.
3. Arquiteto importa o JSON na API com `Idempotency-Key`.
4. Backend materializa inventário, aplica regras, gera achados.
5. Questionário ajusta dimensões; revisão atualiza status + auditoria.
6. Roadmap e relatório; publicação congela snapshot HTML/PDF.

## Camadas do backend

- **domain** — entidades e políticas
- **application** — casos de uso e portas
- **infrastructure** — Panache, filesystem, LLM adapter, métricas, health
- **api** — REST DTOs, filtros (correlation, security headers, rate limit)
- **report** — renderização HTML/PDF

## Segurança por perfil

| Perfil | Auth API | Swagger | TLS OIDC | Fail-fast |
|--------|----------|---------|----------|-----------|
| dev/test/demo | headers locais | ligado (demo/dev) | none | não |
| prod | OIDC + audience/issuer | desligado | required | sim |

## Observabilidade

Ver `docs/OBSERVABILITY.md` — correlationId, logs JSON em prod, métricas Prometheus, live vs ready.

## Princípios

- Dependências apontam para o domínio
- Transações na camada de aplicação
- Entidades JPA não saem na API
- Timestamps UTC; identificadores UUID
- Secrets nunca em defaults de produção
