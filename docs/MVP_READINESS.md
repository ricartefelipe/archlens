# ArchLens — MVP Readiness Matrix

Atualizado em **2026-07-17** (Prompt 5/5 — validação final com gates executados).

**Veredito desta sessão:** `MVP DEMONSTRÁVEL` para demonstração local assistida.  
**Não é pronto para produção** nem para SaaS público.

## Legenda

| Classificação | Significado |
|---------------|-------------|
| `IMPLEMENTED_AND_TESTED` | Existe no código e há evidência de teste/comando nesta validação |
| `IMPLEMENTED_NOT_FULLY_TESTED` | Existe, cobertura parcial ou só caminho alternativo |
| `HEURISTIC` | Comportamento aproximado / regex / confiança limitada |
| `OPTIONAL_EXTERNAL_INTEGRATION` | Fora do caminho principal; desligado por padrão |
| `PLANNED` | Planejado, fora do escopo MVP |
| `BLOCKED` | Impedimento conhecido para piloto/produção |

## Matriz

| Capacidade | Classificação | Evidência / limitação |
|------------|---------------|------------------------|
| Analyzer CLI offline (`scan`/`validate`/`version`) | IMPLEMENTED_AND_TESTED | `./mvnw -B test` (35 testes); `package` + scan fixture → `/tmp/analysis.json` |
| JSON Schema (`contracts/analysis.schema.json`) | IMPLEMENTED_AND_TESTED | `scripts/validate-contracts.sh`; `cli validate`; SchemaSync/SchemaValidator tests |
| Fixture legada `samples/legacy-java-demo` | IMPLEMENTED_AND_TESTED | Scan CI/local; sem vazamento de `demo-not-a-real-secret-123` |
| Hash canônico (`contentHash`) | IMPLEMENTED_AND_TESTED | Campo `contentHash` sha256 no JSON; IT import; restart preservou hash |
| Analyzer não executa alvo | IMPLEMENTED_AND_TESTED | Design + testes SafeFileWalker; sem Maven/Gradle do alvo |
| Sem segredo/fonte no JSON | IMPLEMENTED_AND_TESTED | Secret só como `valueHash`; grep valor fictício falhou (OK); sem source text |
| Backend Quarkus + API jornada | IMPLEMENTED_AND_TESTED | `./mvnw -B test` (20 testes, Postgres/Testcontainers) |
| PostgreSQL real + Liquibase | IMPLEMENTED_AND_TESTED | LiquibaseMigrationIT; Compose Postgres 16; changelog empacotado |
| Authz RBAC (ARCHITECT/VIEWER/ADMIN) | IMPLEMENTED_AND_TESTED | ArchLensApiIT; E2E VIEWER 403; OrgIsolationIT |
| Isolamento por organização | IMPLEMENTED_AND_TESTED | OrgIsolationIT (4); E2E recurso cruzado 404/403 |
| Snapshot imutável / republicar bloqueado | IMPLEMENTED_AND_TESTED | ArchLensApiIT publish 2×; E2E botão Publicar disabled |
| Relatório HTML + PDF válido | IMPLEMENTED_AND_TESTED | IT `%PDF`; E2E download; PDF gerado inicia com `%PDF-1.4` |
| Opção de modernização + roadmap | IMPLEMENTED_AND_TESTED | ArchLensApiIT `modernizationAndRoadmap`; E2E roadmap |
| Questionário + scores/rationale | IMPLEMENTED_AND_TESTED | IT + E2E questionnaire/dimensions |
| Auditoria | IMPLEMENTED_AND_TESTED | E2E página auditoria; eventos IMPORT/REPORT/PUBLISH |
| Frontend Next.js jornada | IMPLEMENTED_AND_TESTED | lint/typecheck/test (14)/build; Playwright 10/10 |
| Compose demo saudável | IMPLEMENTED_AND_TESTED | postgres/keycloak/backend/frontend healthy; ready UP |
| Persistência após restart | IMPLEMENTED_AND_TESTED | `compose down` sem `-v` → assessment+6 findings preservados |
| CI (`.github/workflows/ci.yml`) | IMPLEMENTED_AND_TESTED | Espelhado por `scripts/ci-local.sh` + E2E contra stack |
| Perfil `%prod` fail-fast (OIDC/secrets) | IMPLEMENTED_AND_TESTED | SecurityConfigValidator + docs; validado em Prompt 4 |
| OIDC Bearer ponta a ponta no Compose | IMPLEMENTED_NOT_FULLY_TESTED | Backend prod + Keycloak realm existem; demo usa headers/login local |
| Filtros de findings no servidor | PLANNED | Filtros client-side após GET completo |
| Dashboards Grafana externos | PLANNED | Endpoint `/q/metrics` existe |
| LLM / geração assistida | OPTIONAL_EXTERNAL_INTEGRATION | `ARCHLENS_LLM_ENABLED=false` |
| Multi-tenant SaaS / K8s HA | PLANNED | Fora do MVP |
| Detecção Gradle/sinais avançados | HEURISTIC | Parsers regex; confiança MEDIUM/LOW documentada |
| Rate limit import multi-réplica | HEURISTIC | In-memory por IP; não compartilhado |
| SaaS público multi-cliente | BLOCKED | Ver `COMMERCIAL_READINESS.md` (auth, tenancy, secrets, compliance) |

## Contagens de testes (sessão Prompt 5)

| Módulo | Comando | Resultado |
|--------|---------|-----------|
| Analyzer | `cd analyzer && ./mvnw -B test` | 35 passed, 0 skipped crítico |
| Backend | `cd backend && ./mvnw -B test` | 20 passed (IT Postgres) |
| Frontend unit | `cd frontend && npm run test` | 14 passed |
| Frontend E2E | `ARCHLENS_E2E_REAL=1 PLAYWRIGHT_BASE_URL=http://localhost:3000 PLAYWRIGHT_SKIP_WEBSERVER=1 npm run test:e2e` | 10 passed |

Nota: um teste analyzer usa `@DisabledOnOs(OS.WINDOWS)` (symlink) — skip de plataforma, não omissão de falha.

## Correção residual desta sessão

- Server actions no container Compose falhavam (`fetch failed`) porque `localhost:8080` não alcança o backend.
- Correção: `ARCHLENS_API_INTERNAL_URL=http://backend:8080/api/v1` + `getApiBaseUrl()` server-side.

## O que **não** afirmar

- Precisão absoluta de findings ou scores.
- Pronto para produção / compliance / multi-tenant público.
- OIDC Bearer como caminho padrão do Compose demo.

## Adendo 2026-10-22

Hash canônico na importação, exclusão administrativa, ciclo de symlink, `--exclude`, certeza no relatório, PDF com acento e login local sem senha embutida foram cobertos por teste automatizado nesta data. E2E de browser, OIDC do cliente e restauração do volume real da instalação continuam pendentes. O smoke `scripts/backup-restore-smoke.sh` prova só o caminho da ferramenta em banco efêmero.
