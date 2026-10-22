# ArchLens — Decisões de Arquitetura

## ADR-001: Monólito modular Quarkus

**Decisão:** Backend único Quarkus 3.33 LTS com pacotes `domain` / `application` / `infrastructure` / `api` / `report`.

**Motivo:** MVP com deploy simples (`docker compose`), fronteiras claras sem overhead de microserviços.

## ADR-002: Análise estática sem execução

**Decisão:** Analyzer somente-leitura; nunca invoca Maven/Gradle/JVM do alvo; não segue symlinks fora da raiz.

**Motivo:** Segurança e previsibilidade; código do cliente permanece local.

## ADR-003: Contrato JSON Schema compartilhado

**Decisão:** `contracts/analysis.schema.json` é a fonte de verdade entre CLI e backend.

**Motivo:** Validação na borda; rejeição precoce de imports inválidos.

## ADR-004: Auth via Keycloak OIDC

**Decisão:** Roles `ADMIN`, `ARCHITECT`, `VIEWER` mapeadas de realm roles; uma organização por instalação.

**Motivo:** Padrão enterprise, sem inventar IdP no MVP.

## ADR-005: LLM opcional via porta

**Decisão:** `NarrativeGenerator` com implementação template determinística; adapter OpenAI-compatible só se `LLM_ENABLED=true`. Nunca envia código/segredos.

**Motivo:** MVP funciona offline; narrativa opcional sem acoplamento.

## ADR-006: Snapshots imutáveis de relatório

**Decisão:** Publicação gera HTML/PDF com hash; alterações posteriores não mutam o snapshot.

**Motivo:** Rastreabilidade e auditoria para stakeholders.

## ADR-007: Quarkus 3.33 LTS

**Decisão:** Platform BOM `3.33.2`.

**Motivo:** LTS estável em 2026; suporte longo vs. bleeding edge 3.37+.

## ADR-008: Sem commit automático de rastros de IA

**Decisão:** Commits limpos; sem trailers de co-autoria de ferramentas.

**Motivo:** Política do time / CI.

## ADR-009: Parser Maven XML endurecido (XXE)

**Decisão:** `MavenPomParser` usa DOM com DTD desabilitada, entidades externas off e sem resolução de rede.

**Motivo:** POMs não confiáveis não podem ler o filesystem do operador via XXE.

## ADR-010: Gradle apenas heurístico

**Decisão:** `GradleBuildParser` não avalia Groovy/Kotlin nem executa Gradle; version catalogs ficam não resolvidos.

**Motivo:** Segurança e previsibilidade offline; limitações documentadas em `LIMITATIONS.md`.

## ADR-011: Indicador de segredo contextual

**Decisão:** `valueHash` de segredos é `SHA-256(domínio + tipo + path + linha + valor)`, nunca hash nu do valor; evidências não guardam match bruto.

**Motivo:** Evitar correlação indevida do mesmo segredo entre análises/caminhos e vazamento em JSON/logs.

## ADR-012: Testes de integração em PostgreSQL real

**Decisão:** A suíte Quarkus usa Testcontainers (`PostgresTestResource` + `postgres:16-alpine`) e Liquibase; H2 permanece só no profile `%dev` para bootstrap local rápido.

**Motivo:** Provar UUID/JSONB/BYTEA/constraints/índices no dialeto real; H2 não é prova de compatibilidade PostgreSQL.

## ADR-013: Isolamento organizacional e 404

**Decisão:** Leituras/escritas por UUID exigem `organization_id` (e `assessment_id` quando aplicável). Header opcional `X-Organization-Id` seleciona o tenant; acesso cruzado responde **404** (sem revelar existência). Inventário também carrega `organization_id`.

**Motivo:** Defesa em profundidade e testes negativos com duas organizações.

## ADR-014: TEXT/BYTEA em vez de @Lob/OID

**Decisão:** Campos longos (`raw_json`, HTML, descrições) usam `TEXT`; PDF usa `BYTEA`. Evitar `@Lob` → OID no PostgreSQL.

**Motivo:** OID exige conexão fora de auto-commit e quebra leituras Panache pós-transação.

## ADR-015: Idempotência de import e raw_json

**Decisão:** Mesma `(org, assessment, Idempotency-Key)` + mesmo `contentHash` → replay 200; hash diferente → **409**. `raw_json` persiste apenas `analysis.json` (sem fonte), limitado por `archlens.import.max-bytes` (default 5 MiB).

**Motivo:** Auditoria de import sem armazenar código-fonte; conflito explícito evita sobrescrita silenciosa.

## ADR-016: Scores como apoio à decisão

**Decisão:** Scores são determinísticos (base 50 ± findings ± questionário), clamp 0–100, com disclaimer em rationale/relatório. Não são precisão científica.

**Motivo:** Transparência para stakeholders e alinhamento ao questionário versionado.

## ADR-017: Perfis demo vs produção

**Decisão:** Compose padrão usa `QUARKUS_PROFILE=demo` com header auth explícito. Perfil `%prod` exige OIDC, TLS verification, CORS restritivo, sem header auth, sem defaults de secret e falha no startup se inseguro (`SecurityConfigValidator`).

**Motivo:** Conveniência local visível e separada de hardening real; evita “prod falso” no Compose.

## ADR-018: Observabilidade mínima embutida

**Decisão:** CorrelationId + MDC, métricas Micrometer/Prometheus (`archlens_*`), health live/ready com check de regras, logs JSON só em `%prod`. Tracing OTel fica opcional/planejado.

**Motivo:** Operabilidade do MVP sem inventar stack Grafana/Kafka.

## ADR-019: Rate limit in-memory na importação

**Decisão:** Filtro JAX-RS limita POSTs de import por minuto por IP/usuário. Em produção multi-instância, documentar substituição por gateway.

**Motivo:** Mitigar abuso de upload no MVP sem dependência externa.

## ADR-020: Imagens e Compose com versões fixas + non-root

**Decisão:** Tags pinadas (`postgres:16.6-alpine`, Keycloak `26.0.7`, Node/Temurin fixos), Dockerfiles multi-stage, usuário não-root, volumes nomeados, `down` sem `-v` nos scripts oficiais.

**Motivo:** Reproducibilidade e menor superfície; preservar dados em reinícios.

## ADR-021: Hash canônico conferido na importação

**Decisão:** O backend recalcula o `contentHash` com a mesma canonicalização do CLI (propriedades ordenadas, sem `generatedAt`, `durationMs` e o próprio hash). Divergência rejeita o import. A única `schemaVersion` aceita é `1.0.0`. Não há migração silenciosa: uma versão nova exige CLI e backend publicados juntos e continua fail-closed para a versão desconhecida.

**Motivo:** JSON editado à mão não pode herdar o hash de outra análise. O contrato deixa de ser só formato e passa a ser integridade.

## ADR-022: Retirada administrativa não é edição do snapshot

**Decisão:** Snapshot publicado continua imutável para correção. A exclusão da avaliação, restrita a ADMIN, apaga inclusive HTML/PDF e redige a auditoria. Acesso de outra organização segue 404.

**Motivo:** Imutabilidade protege a entrega; o offboarding contratual precisa poder apagar o dado. São operações diferentes.

## ADR-023: Login local sem senha embutida

**Decisão:** Não há fallback `admin123` no código. Senha vem de ambiente. Senhas de demonstração conhecidas só autenticam com `ARCHLENS_ALLOW_DEMO_CREDENTIALS=true`. `ARCHLENS_PROFILE_LABEL=production` desliga o login local mesmo que a flag de auth local esteja ligada. O build da imagem não define `AUTH_LOCAL_ENABLED`.

**Motivo:** Instalação de cliente não pode herdar credencial de demo por omissão. A demo continua explícita no Compose.

## ADR-024: Relatório separa detecção e revisão

**Decisão:** Achados guardam origem `AUTOMATED` e certeza `STRUCTURAL` ou `HEURISTIC`. O HTML/PDF tem as duas seções, disclaimer de que não há nota única, e fonte DejaVu Sans embutida para português. O hash do snapshot é o do HTML; adulteração do HTML falha no download.

**Motivo:** O patrocinador precisa ver o que a regra produziu e o que o arquiteto decidiu, inclusive com acentos no PDF.

