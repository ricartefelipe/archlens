# Arquitetura — ArchLens

## Visão

```
┌─────────────┐     analysis.json      ┌──────────────────┐
│ Analyzer CLI│ ─────────────────────► │ Backend Quarkus  │
│ (local)     │                        │ domain/app/infra │
└─────────────┘                        └────────┬─────────┘
                                                │ REST + OIDC
┌─────────────┐                        ┌────────▼─────────┐
│ Sample demo │                        │ Frontend Next.js │
└─────────────┘                        └────────┬─────────┘
                                                │
                                       ┌────────▼─────────┐
                                       │ Keycloak + PG    │
                                       └──────────────────┘
```

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
- **infrastructure** — Panache, filesystem, LLM adapter
- **api** — REST DTOs, segurança
- **report** — renderização HTML/PDF

## Princípios

- Dependências apontam para o domínio
- Transações na camada de aplicação
- Entidades JPA não saem na API
- Timestamps UTC; identificadores UUID
