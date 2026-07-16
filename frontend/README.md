# ArchLens Frontend

Interface Next.js da jornada de diagnóstico arquitetural.

## Requisitos

- Node.js 20+
- Backend em `http://localhost:8080/api/v1` (Quarkus)
- Keycloak local em `http://localhost:8180/realms/archlens` (via `infra/`)

## Configuração

```bash
cp .env.example .env.local
npm install
npm run dev
```

Abra [http://localhost:3000](http://localhost:3000).

### Credenciais locais (dev)

| Usuário     | Senha     | Papel      |
|-------------|-----------|------------|
| admin       | admin     | ADMIN      |
| architect   | architect | ARCHITECT  |
| viewer      | viewer    | VIEWER     |

VIEWER tem acesso somente leitura; ARCHITECT/ADMIN podem alterar achados, questionário e roadmap.

## Scripts

| Comando           | Descrição                    |
|-------------------|------------------------------|
| `npm run dev`     | Servidor de desenvolvimento  |
| `npm run build`   | Build de produção            |
| `npm run start`   | Servidor de produção         |
| `npm run lint`    | ESLint                       |
| `npm run test:e2e`| Playwright smoke tests       |

## Testes E2E

Smoke tests em `e2e/smoke.spec.ts`. Sem Keycloak configurado, o fluxo OIDC é ignorado (`test.skip`).

```bash
npm run test:e2e
```

Com stack completa, defina `KEYCLOAK_ISSUER` e `KEYCLOAK_CLIENT_ID` no ambiente antes de rodar os testes.

## Jornada

`/` → login → assessments → import → inventory → findings → questionnaire → dimensions → roadmap → report → audit
