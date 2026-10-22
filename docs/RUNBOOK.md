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

**Comando recomendado** (detecta Desktop inativo / stack já no sistema):

```bash
./scripts/stack-up.sh
# parar sem apagar volumes:
./scripts/stack-down.sh
```

O script define `DOCKER_HOST=unix:///var/run/docker.sock` quando o sock do Desktop
não responde, quando a stack `infra-*`/`archlens-*` já está no Engine do sistema,
ou quando o contexto ativo é `desktop-linux` (evita segunda cópia na 8081).

Equivalente manual:

```bash
export DOCKER_HOST=unix:///var/run/docker.sock
docker compose -f infra/docker-compose.yml up --build -d
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

1. Subir stack (`./scripts/stack-up.sh` / `.\scripts\stack-up.ps1`).
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

O dump inclui avaliações, `raw_json`, HTML/PDF e a fila `blob_cleanup_task`. Restaure em um Postgres 16 vazio e suba o backend para o Liquibase reconhecer o changelog já aplicado.

Ensaio de 22/10/2026, num projeto Compose descartável `archlens-closure` (não é o volume de outro produto): importação do fixture demo, geração de relatório, `pg_dump` e restore num segundo Postgres 16. A API apontada para a base restaurada devolveu a avaliação, 6 achados e os eventos de auditoria. Exclusão da avaliação não apaga a cópia que já estiver num backup. Com `ARCHLENS_RETAIN_AUDIT=true` (padrão), a trilha também permanece no banco de origem.

Não use `down -v` no meio do ensaio, nem no projeto Compose de outra pessoa.

Excluir uma avaliação (ADMIN, com confirmação na UI) remove inventário, achados, questionário, scores, roadmap, importações e o vínculo do relatório. O PDF deixa de ser baixável na hora. Se a limpeza do blob falhar, repita `POST /api/v1/maintenance/blob-cleanup`. Com retenção de auditoria, os eventos continuam em `GET /api/v1/audit`.

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
| `ports are not available` / `8081: bind: address already in use` | Quase sempre a stack já está no sock do sistema enquanto o CLI usa Desktop. Use `./scripts/stack-up.sh` (ou `DOCKER_HOST=unix:///var/run/docker.sock`) e `docker compose -f infra/docker-compose.yml ps`. Se houver órfãos no Desktop: `docker compose -p archlens down` (sem `-v`). Não mate outros projetos. |
| Build frontend transfere ~1GB | Falta `.dockerignore` no contexto (`frontend/.dockerignore` deve excluir `node_modules` e `.next`). Confirme que está na cópia certa do repo. |
| Login falha | Realm importado? Porta 8081 livre? `AUTH_LOCAL_ENABLED=true` no demo? |
| Login E2E falha com `127.0.0.1` | Use `http://localhost:3000` (cookies/`NEXTAUTH_URL`) |
| UI `fetch failed` / lista vazia no Compose | Confirme `ARCHLENS_API_INTERNAL_URL=http://backend:8080/api/v1` no serviço frontend |
| Import 400 | JSON fora do schema, ou `contentHash` divergente do algoritmo `archlens-content-hash/v1`. Rode `validate` e reenvie o arquivo gerado pelo CLI |
| Import 409 | A mesma `Idempotency-Key` já foi usada com outro hash |
| Exclusão não apaga o PDF na hora | A linha do snapshot fica órfã até `POST /api/v1/maintenance/blob-cleanup` (ADMIN). O download já responde 404 |
| Exclusão 400 retenção | `ARCHLENS_LEGAL_HOLD=true` bloqueia o delete. Auditoria permanece se `ARCHLENS_RETAIN_AUDIT=true` (padrão) |
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
