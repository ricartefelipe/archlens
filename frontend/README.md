# ArchLens Frontend

Interface Next.js da jornada de diagnóstico arquitetural.

## Requisitos

- Node.js 22+
- Backend em `http://localhost:8080/api/v1` (Quarkus)
- Keycloak local em `http://localhost:8081/realms/archlens` (via `infra/`) — opcional se `AUTH_LOCAL_ENABLED=true`

## Configuração

```bash
cp .env.example .env.local
npm ci
npm run dev
```

Abra [http://localhost:3000](http://localhost:3000).

### Autenticação

- **Login local**: somente com `AUTH_LOCAL_ENABLED=true`, `ARCHLENS_PROFILE_LABEL` diferente de `production` e senhas em `AUTH_LOCAL_*_PASSWORD`. As senhas de demonstração também exigem `ARCHLENS_ALLOW_DEMO_CREDENTIALS=true`. Não há senha padrão no código.
- **Keycloak/OIDC**: defina `KEYCLOAK_ISSUER`, `KEYCLOAK_CLIENT_ID` e `KEYCLOAK_CLIENT_SECRET`.
- Em instalação de cliente, login local fica desligado e `AUTH_SECRET` é forte. ADMIN pode retirar os dados da avaliação; ARCHITECT revisa; VIEWER só lê.

## Scripts

| Comando            | Descrição                         |
|--------------------|-----------------------------------|
| `npm run dev`      | Servidor de desenvolvimento       |
| `npm run build`    | Build de produção                 |
| `npm run start`    | Servidor de produção              |
| `npm run lint`     | ESLint                            |
| `npm run typecheck`| TypeScript (`tsc --noEmit`)       |
| `npm test`         | Vitest (unitário)                 |
| `npm run test:e2e` | Playwright                        |

## Testes E2E (stack real)

```bash
# Terminal 1 — infra
set DOCKER_HOST=unix:///var/run/docker.sock
docker compose -f infra/docker-compose.yml up --build -d postgres keycloak backend

# Terminal 2 — analysis.json real (opcional; fixture também serve)
cd analyzer
java -jar cli/target/archlens-analyzer-cli-*.jar scan --path ../samples/legacy-java-demo --out /tmp/archlens-e2e-analysis.json

# Terminal 3 — E2E
cd frontend
set ARCHLENS_E2E_REAL=1
set AUTH_LOCAL_ENABLED=true
set ARCHLENS_ANALYSIS_JSON=/tmp/archlens-e2e-analysis.json
set NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
npm run test:e2e
```

## Jornada

`/` → login → assessments → import → inventory → findings → questionnaire → dimensions → roadmap → report → audit → logout
