# Runbook — ArchLens

## Perfis

- **Demo local (padrão Compose):** `QUARKUS_PROFILE=demo`, auth por headers + login local no frontend. **Não é produção.**
- **Produção:** ver `docs/ENVIRONMENTS.md` e `infra/docker-compose.prod.example.yml`. O startup **falha** se OIDC/secrets/CORS estiverem inseguros.

## Windows — subir e parar sem apagar dados

PowerShell (na raiz do repositório):

```powershell
.\scripts\stack-up.ps1
# ... uso ...
.\scripts\stack-down.ps1
```

Equivalente manual:

```powershell
docker compose -f infra\docker-compose.yml up --build -d
docker compose -f infra\docker-compose.yml down
```

**Não** use `-v` a menos que queira apagar o volume `pgdata` (ex.: `infra_pgdata`).

## Linux/macOS — subir stack

Neste ambiente a stack demo costuma rodar no Docker Engine do sistema
(`unix:///var/run/docker.sock`). Se o contexto ativo for **Docker Desktop**
(`desktop-linux`), um `docker compose up` tenta outro daemon e falha com
`bind: address already in use` nas portas 3000/8080/8081/5432.

Comando recomendado (força o sock do sistema):

```bash
export DOCKER_HOST=unix:///var/run/docker.sock
docker compose -f infra/docker-compose.yml up --build -d
# ou na raiz (include → infra/):
docker compose up --build -d
```

Alternativa: `docker context use default` (endpoint `unix:///var/run/docker.sock`).

Para limpar containers órfãos criados pelo Desktop **sem** apagar volumes:

```bash
# no contexto Desktop (sem DOCKER_HOST), projeto típico "archlens":
docker compose -p archlens down
# NÃO use -v
```

Contexto de build: frontend usa `frontend/` + `frontend/.dockerignore`
(exclui `node_modules`/`.next`); backend usa a raiz + `.dockerignore` na raiz.

| Serviço | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend ready | http://localhost:8080/q/health/ready |
| Backend live | http://localhost:8080/q/health/live |
| Metrics | http://localhost:8080/q/metrics |
| API health | http://localhost:8080/api/v1/health |
| Keycloak | http://localhost:8081 |
| Postgres | localhost:5432 |

## Credenciais fictícias (somente demo)

| Usuário | Senha | Papéis |
|---------|-------|--------|
| admin | admin123 | ADMIN, ARCHITECT, VIEWER |
| architect | architect123 | ARCHITECT, VIEWER |
| viewer | viewer123 | VIEWER |

Keycloak admin console: `admin` / `admin` (realm `archlens`).  
Client secrets de demo: `archlens-frontend-secret`, `archlens-api-secret` — **não reutilizar em produção**.

## Procedimento de demo (curto)

1. Subir stack (`stack-up` / `docker compose up --build -d`).
2. Confirmar ready: `curl http://localhost:8080/q/health/ready`.
3. Login em http://localhost:3000 com `architect` / `architect123`.
4. Gerar analysis.json (abaixo) e importar na UI.
5. Percorrer inventário → findings → questionário → scores → roadmap → publicar relatório.
6. Validar VIEWER: login `viewer` — leitura ok, escrita bloqueada.
7. Parar **sem** `-v`.

## Scan do sample (analyzer offline)

```bash
cd analyzer
./mvnw -pl cli -am package -DskipTests
java -jar cli/target/archlens-analyzer-cli-*.jar scan --path ../samples/legacy-java-demo --out /tmp/analysis.json
java -jar cli/target/archlens-analyzer-cli-*.jar validate --file /tmp/analysis.json
# Garantir que o valor fictício não vazou:
grep -F 'demo-not-a-real-secret-123' /tmp/analysis.json && echo FAIL || echo OK
```

## Backup / restore do Postgres

```bash
# Backup (stack rodando)
docker compose -f infra/docker-compose.yml exec -T postgres \
  pg_dump -U archlens archlens > archlens-backup.sql

# Restore
docker compose -f infra/docker-compose.yml exec -T postgres \
  psql -U archlens -d archlens < archlens-backup.sql
```

Volume Compose: `pgdata` (ex.: `infra_pgdata` quando o projeto é `infra`). Reinício da stack preserva dados.

## Reinício controlado

```bash
docker compose -f infra/docker-compose.yml up -d --build --force-recreate
# volumes preservados
```

## Problemas comuns

| Sintoma | Ação |
|---------|------|
| Backend unhealthy | `docker compose -f infra/docker-compose.yml logs backend`; Postgres ready? |
| Startup falha em prod | Ver mensagem do `SecurityConfigValidator` (OIDC/secrets/CORS) |
| `ports are not available` / `8081: bind: address already in use` | Quase sempre a stack já está no sock do sistema enquanto o CLI usa Desktop. Use `DOCKER_HOST=unix:///var/run/docker.sock` e `docker compose -f infra/docker-compose.yml ps`. Não mate outros projetos. |
| Build frontend transfere ~1GB | Falta `.dockerignore` no contexto (`frontend/.dockerignore` deve excluir `node_modules` e `.next`). Confirme que está na cópia certa do repo. |
| Login falha | Realm importado? Porta 8081 livre? `AUTH_LOCAL_ENABLED=true` no demo? |
| Login E2E falha com `127.0.0.1` | Use `http://localhost:3000` (cookies/`NEXTAUTH_URL`) |
| UI `fetch failed` / lista vazia no Compose | Confirme `ARCHLENS_API_INTERNAL_URL=http://backend:8080/api/v1` no serviço frontend |
| Import 400 | JSON fora do schema — rode `validate`; API exige `Idempotency-Key` |
| Import 409 | Troque `Idempotency-Key` ou reenvie o mesmo hash |
| Import 429 | Rate limit — aguarde 1 minuto |
| Liquibase lock | `UPDATE databasechangeloglock SET locked=false WHERE id=1;` (um único backend) |
| VIEWER consegue escrever | Bug — reportar; em demo headers devem refletir papéis da sessão |

## Quality gate local

```bash
bash scripts/ci-local.sh
```

E2E contra Compose (frontend já no ar; use **localhost**, não 127.0.0.1):

```bash
export ARCHLENS_E2E_REAL=1
export PLAYWRIGHT_BASE_URL=http://localhost:3000
export PLAYWRIGHT_SKIP_WEBSERVER=1
export ARCHLENS_ANALYSIS_JSON=/tmp/analysis.json   # gerado pelo CLI
cd frontend && npm run test:e2e
```

## Desligar

```bash
docker compose -f infra/docker-compose.yml down          # preserva volumes
# docker compose -f infra/docker-compose.yml down -v    # APAGA dados — só se intencional
```
