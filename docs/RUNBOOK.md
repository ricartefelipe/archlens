# Runbook — ArchLens local

## Subir stack

```bash
docker compose up --build
```

Serviços:

| Serviço | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Health | http://localhost:8080/q/health |
| Keycloak | http://localhost:8081 |
| Postgres | localhost:5432 |

## Credenciais locais

| Usuário | Senha | Papéis |
|---------|-------|--------|
| admin | admin123 | ADMIN, ARCHITECT, VIEWER |
| architect | architect123 | ARCHITECT, VIEWER |
| viewer | viewer123 | VIEWER |

Keycloak admin console: `admin` / `admin` (realm `archlens`).

## Scan do sample

```bash
cd analyzer
./mvnw -pl cli -am package -DskipTests
java -jar cli/target/archlens-cli-*.jar scan --path ../samples/legacy-java-demo --out /tmp/analysis.json
java -jar cli/target/archlens-cli-*.jar validate --file /tmp/analysis.json
```

Importe o arquivo pela UI em **Avaliação → Importar**.

## Problemas comuns

| Sintoma | Ação |
|---------|------|
| Backend unhealthy | Ver logs `docker compose logs backend`; Postgres ready? |
| Login falha | Keycloak importou realm? Porta 8081 livre? |
| Import 400 | JSON fora do schema — rode `validate` |
| Import 409/idempotente | Reutilize ou troque `Idempotency-Key` |

## Desligar

```bash
docker compose down
# com volume: docker compose down -v
```
