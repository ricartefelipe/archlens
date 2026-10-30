# ArchLens

Plataforma de **diagnóstico arquitetural** e **planejamento de modernização** para aplicações Java legadas.

Valor: transformar um sistema pouco conhecido em diagnóstico rastreável e plano executável — **sem enviar código-fonte para fora**.

> **Status:** MVP **demonstrável** (jornada local validada). O Compose padrão é **demonstração local**, não produção. Ver [MVP_READINESS](docs/MVP_READINESS.md).

## Início rápido (comprovado no Compose demo)

### Pré-requisitos

- Docker + Docker Compose
- Java 21 e Maven (CLI local)
- Node 22+ (frontend em modo dev)

### Windows (PowerShell) — sobe e para sem apagar dados

```powershell
.\scripts\stack-up.ps1
# App: http://localhost:3000
.\scripts\stack-down.ps1
```

### Linux / macOS

Se o contexto Docker ativo for Desktop (`desktop-linux`) e a stack já estiver
no Engine do sistema, use o sock explícito para evitar conflito de portas
(3000/8080/8081/5432):

```bash
export DOCKER_HOST=unix:///var/run/docker.sock
docker compose -f infra/docker-compose.yml up --build -d
# ready:
curl -fsS http://localhost:8080/q/health/ready
```

Detalhes e limpeza de órfãos do Desktop: [RUNBOOK](docs/RUNBOOK.md).

| Serviço | URL |
|---------|-----|
| App | http://localhost:3000 |
| API ready | http://localhost:8080/q/health/ready |
| Metrics | http://localhost:8080/q/metrics |
| Keycloak | http://localhost:8081 |

**Usuários fictícios (demo):** `admin`/`admin123`, `architect`/`architect123`, `viewer`/`viewer123`

O backend no Compose usa perfil **`demo`** (auth por headers). Instalação do operador na própria máquina usa perfil **`local`**. Produção exige perfil `%prod` + secrets reais — ver [ENVIRONMENTS](docs/ENVIRONMENTS.md). Headers `X-User` / `X-Roles` / `X-Organization-Id` são ignorados em produção.

### Analisar o sample (offline)

```bash
cd analyzer && ./mvnw -pl cli -am package -DskipTests
java -jar cli/target/archlens-analyzer-cli-*.jar scan \
  --path ../samples/legacy-java-demo \
  --out /tmp/analysis.json
java -jar cli/target/archlens-analyzer-cli-*.jar validate --file /tmp/analysis.json
```

Depois: login → criar avaliação → importar `/tmp/analysis.json` → questionário → revisar achados → roadmap → publicar relatório.

## Módulos

| Pasta | Descrição |
|-------|-----------|
| `analyzer/` | CLI de análise estática (`scan`, `validate`, `version`) |
| `backend/` | API Quarkus, regras, relatório, auditoria |
| `frontend/` | UI Next.js (jornada completa) |
| `contracts/` | JSON Schema + OpenAPI |
| `infra/` | Compose, Keycloak, overlay prod exemplo |
| `scripts/` | CI local, scan de secrets, stack Windows |
| `samples/legacy-java-demo/` | Fixture legada |
| `docs/` | Plano, ADRs, threat model, runbook, ambientes |

## Documentação

- [Ambientes e config](docs/ENVIRONMENTS.md)
- [Runbook](docs/RUNBOOK.md)
- [Observabilidade](docs/OBSERVABILITY.md)
- [Arquitetura](docs/ARCHITECTURE.md)
- [Threat model](docs/THREAT_MODEL.md)
- [Decisões](docs/DECISIONS.md)
- [Tasks](docs/TASKS.md)
- [Limitações](docs/LIMITATIONS.md)
- [MVP Readiness](docs/MVP_READINESS.md)
- [Roteiro de demo](docs/DEMO_SCRIPT.md)
- [Prontidão comercial](docs/COMMERCIAL_READINESS.md)

## CI

Pipeline em `.github/workflows/ci.yml` (push/PR): analyzer, secret-scan, backend+IT, frontend, contracts, compose config, build e scan bloqueante (HIGH/CRITICAL) das imagens finais, E2E com API real e E2E de login OIDC real no Keycloak (PKCE, renovação e revogação). Sem deploy.

Equivalente local: `bash scripts/ci-local.sh`

## Licença

A licença de distribuição **ainda não foi escolhida** pelo proprietário. Não há cessão de direitos neste repositório. Inventário e uma proposta para decisão estão em [LICENSE_INVENTORY](docs/LICENSE_INVENTORY.md) e [COMMERCIAL_TERMS_PROPOSAL](docs/COMMERCIAL_TERMS_PROPOSAL.md). Posicionamento da oferta: [COMMERCIAL_READINESS](docs/COMMERCIAL_READINESS.md). Fechamento desta revisão: [CLOSURE_REPORT](docs/CLOSURE_REPORT.md).
