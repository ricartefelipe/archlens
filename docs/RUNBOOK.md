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

**Não** use `-v` a menos que queira apagar o volume `archlens_pgdata`.

Se o Docker Desktop estiver inativo no Linux/WSL:

```bash
export DOCKER_HOST=unix:///var/run/docker.sock
```

## Linux/macOS — subir stack

```bash
docker compose -f infra/docker-compose.yml up --build -d
# ou na raiz:
docker compose up --build -d
```

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
| Login falha | Realm importado? Porta 8081 livre? `AUTH_LOCAL_ENABLED=true` no demo? |
| Import 400 | JSON fora do schema — rode `validate` |
| Import 409 | Troque `Idempotency-Key` ou reenvie o mesmo hash |
| Import 429 | Rate limit — aguarde 1 minuto |
| Liquibase lock | `UPDATE databasechangeloglock SET locked=false WHERE id=1;` (um único backend) |
| VIEWER consegue escrever | Bug — reportar; em demo headers devem refletir papéis da sessão |

## Quality gate local

```bash
bash scripts/ci-local.sh
```

## Desligar

```bash
docker compose -f infra/docker-compose.yml down          # preserva volumes
# docker compose -f infra/docker-compose.yml down -v    # APAGA dados — só se intencional
```
