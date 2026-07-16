# ArchLens

Plataforma de **diagnóstico arquitetural** e **planejamento de modernização** para aplicações Java legadas.

Valor: transformar um sistema pouco conhecido em diagnóstico rastreável e plano executável — **sem enviar código-fonte para fora**.

## Início rápido

### Pré-requisitos

- Docker + Docker Compose
- Java 21 e Maven (para CLI local)
- Node 22+ (desenvolvimento frontend)

### Subir o ambiente

```bash
docker compose up --build
```

- App: http://localhost:3000  
- API: http://localhost:8080/q/health  
- Keycloak: http://localhost:8081  

**Usuários (login local / Keycloak):** `admin`/`admin123`, `architect`/`architect123`, `viewer`/`viewer123`

### Analisar o sample

```bash
cd analyzer && ./mvnw -pl cli -am package -DskipTests
java -jar cli/target/archlens-cli-*.jar scan \
  --path ../samples/legacy-java-demo \
  --out /tmp/analysis.json
java -jar cli/target/archlens-cli-*.jar validate --file /tmp/analysis.json
```

Depois: login → criar avaliação → importar `/tmp/analysis.json` → questionário → revisar achados → roadmap → publicar relatório.

## Módulos

| Pasta | Descrição |
|-------|-----------|
| `analyzer/` | CLI de análise estática (`scan`, `validate`, `version`) |
| `backend/` | API Quarkus, regras, relatório, auditoria |
| `frontend/` | UI Next.js (jornada completa) |
| `contracts/` | JSON Schema + OpenAPI |
| `infra/` | Compose, Keycloak |
| `samples/legacy-java-demo/` | Fixture legada |
| `docs/` | Plano, ADRs, threat model, runbook |

## Documentação

- [Plano de implementação](docs/IMPLEMENTATION_PLAN.md)
- [Tasks](docs/TASKS.md)
- [Decisões](docs/DECISIONS.md)
- [Arquitetura](docs/ARCHITECTURE.md)
- [Threat model](docs/THREAT_MODEL.md)
- [Runbook](docs/RUNBOOK.md)
- [Regras](docs/RULES.md)
- [Questionário](docs/QUESTIONNAIRE.md)

## Licença

Uso interno / MVP — definir licença antes de distribuição pública.
