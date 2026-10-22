# Fechamento ArchLens — 22/10/2026

Trabalho local na branch `feature/melhorias-archlens`, commit de base `c68450c3be1b48d53bc07998926da0d59b026677`. Nenhuma ref Git foi criada, movida ou publicada. As alterações estão no working tree para revisão do proprietário.

## Resumo

O build do analisador quebrava porque `ProjectAnalyzer` referencia `MavenPomParser` e `GradleBuildParser`, e `**/build/` no `.gitignore` impedia versionar o pacote Java `io.archlens.analyzer.core.build`. Os parsers não estavam no disco. Foram implementados de verdade (XML endurecido contra XXE; Gradle heurístico, sem executar build) e o ignore passou a cobrir só saídas Gradle nos primeiros níveis de diretório.

A importação passou a recalcular `archlens-content-hash/v1`, rejeitar hash divergente e deduplicar por `(organização, avaliação, contentHash)`, com alias de idempotência para não perder o conflito 409. A exclusão de avaliação remove os dependentes, torna o PDF inacessível e deixa a limpeza do blob retomável. Produção ignora headers de identidade e de organização. A UI de ADMIN confirma a exclusão.

## Matriz

| ID | Prioridade | Evidência | Decisão | Estado |
|----|------------|-----------|---------|--------|
| ARCHLENS-01 | P0 | `ProjectAnalyzer` importava parsers ausentes; `git check-ignore` apontava `.gitignore:6:**/build/` | Parsers reais e ignore corrigido | Resolvido. Testes `BuildParsersTest`, `ProjectAnalyzerTest`, CLI |
| ARCHLENS-02 | P0 | `AnalysisImportService` gravava `doc.contentHash()` sem recalcular | Recálculo, rejeição e unicidade por organização e avaliação | Resolvido. `ImportIntegrityIT`, `ClosurePolicyTest` |
| ARCHLENS-03 | P1 | README descrevia demo por headers; licença indefinida | Perfis local/demo/prod; produção ignora headers; proposta de termos sem adotar licença | Resolvido no código e na documentação. A escolha da licença continua com o proprietário |
| ARCHLENS-04 | P1 | `AssessmentService.delete` só removia a avaliação | Exclusão transacional dos dependentes, hold legal, auditoria configurável, fila de blob | Resolvido. `AssessmentDeletionIT` |
| Ingestão | P1 | Walker já bloqueava symlink externo; faltavam profundidade, tempo, cancelamento, segredo e vendor | Limites e diagnóstico no stderr do `scan` | Resolvido. `SafeFileWalkerTest` |
| Relatório | P1 | HTML já tinha resumo e limitações; hipótese podia parecer fato | Seções de diagnóstico, evidência e incerteza; achados rotulados como candidatos | Resolvido. IT de relatório existente continua verde |
| Inventário grande | P1 | Lista sem teto | `offset`/`limit` (máx. 1000) com consulta paginada e escopo da organização | Resolvido |
| Regras | P1 | Schema `1.0.0` já existia; regras sem versão de conjunto | `rules/ruleset.properties` versão `1.0.0` exposta em `/api/v1/health` | Resolvido |
| E2E | P1 | Specs reais, sem `skip` | Jornada, negativos e smoke contra API demo no host | Resolvido nesta máquina. 10 testes Playwright, 22/10/2026. A CI remota continua pendente de publicação |
| Identidade da API | P0 | `GET /api/v1/assessments` respondia 200 sem chamar `IdentityService` | `ApiIdentityFilter` exige identidade em `/api/v1`, exceto `/api/v1/health` | Resolvido. `ClosedApiIdentityIT` e processo `%prod` (401 sem token e com header forjado) |
| Corrida de importação | P0 | Constraint única sem teste de threads | `ImportConcurrencyIT` com 8 requisições simultâneas no Postgres | Resolvido. Uma linha; a chave perdedora fica no alias e um payload diferente nessa chave responde 409 |
| Limite de importação | P1 | `X-Forwarded-For` escolhia o balde do limite | Só entra no balde se `ARCHLENS_RATE_LIMIT_TRUST_FORWARDED=true` | Resolvido no código. O limite em cluster continua em memória |
| Offline / narrativa | P1 | `ARCHLENS_LLM_ENABLED=false` por padrão; o adapter não chamava rede | Opt-in com timeout, teto de tokens e prompt só com contagens; falha volta ao template | Resolvido no código. Chamada real a provedor **não** executada |
| Licença | P1 | README dizia “definir licença” | Inventário e proposta, sem escolher direitos | Pendente de decisão do proprietário |

## Cenários de aceite

| Cenário | Resultado nesta sessão |
|---------|------------------------|
| Analisador, backend e contratos compilam; parsers não são stub | Analisador: 28 testes (core 24 + CLI 4). Backend: 41 testes, PostgreSQL Testcontainers. Frontend: lint, typecheck, 14 testes Vitest e `next build` 15.5.28 |
| Mesmo projeto e mesmas regras, hash estável; Maven multi-módulo e Gradle com limite explícito | `contentHashIsDeterministic`, `BuildParsersTest`, fixture demo confere o hash. SHA-256 não autentica a origem |
| Dois envios do mesmo conteúdo não duplicam a importação; hash adulterado é 400; outra organização não lê o artefato | `ImportIntegrityIT`, `ImportConcurrencyIT` e `OrgIsolationIT` |
| Symlink externo, profundidade, arquivo sensível e cancelamento | `SafeFileWalkerTest`. Repositório “enorme” não foi gerado; o teto de 50k arquivos / 200 MiB / 60 s está no walker |
| Exclusão some com dependentes e com o download; limpeza de blob roda de novo | `AssessmentDeletionIT`: órfãos zerados, falha de armazenamento deixa a tarefa `PENDING`, a tentativa seguinte conclui. Hold legal e rollback por ator longo demais também cobertos |
| Fluxo com a stack local | Playwright `ARCHLENS_E2E_REAL=1`: 10 testes, inclusive cancelar e confirmar a exclusão. O frontend de desenvolvimento falou com a API em `localhost:8080` |
| Produção rejeita header forjado | Processo `%prod`: config ausente não sobe; senha `archlens`, header, TLS trust-all, segredo de demo, issuer localhost e CORS `*` são recusados pelo `SecurityConfigValidator`. Com config aceita e OIDC desligado só no tenant, `GET /api/v1/assessments` sem token ou com header forjado responde 401 |

## Gates

| Gate | Resultado |
|------|-----------|
| `analyzer/./mvnw -B -pl cli -am test` | Sucesso. 28 testes. ~5 s |
| `backend/./mvnw -B test` | Sucesso. 41 testes, 0 falhas, 0 ignorados. Postgres 16 via Testcontainers. ~61 s na execução final (`DOCKER_HOST=unix:///var/run/docker.sock`) |
| `frontend`: lint, typecheck, Vitest | Sucesso. 14 testes, depois do bump de Next 15.5.28, next-auth 5.0.0-beta.32 e mermaid 11.16.1 |
| `frontend`: `npm run build` | Sucesso. Next.js 15.5.28. Aviso do `jose` no Edge Runtime permanece |
| `bash scripts/validate-contracts.sh` | Sucesso na passagem anterior desta data. O schema não mudou depois |
| Playwright `ARCHLENS_E2E_REAL=1` | Sucesso local. 10 testes, 58 s. API era o jar atual em demo, não o container antigo. Chromium instalado nesta máquina |
| Restore | Ensaio feito. Dump de 112871 bytes do Postgres do projeto `archlens-closure`, restore num Postgres 16 vazio, API nova leu a avaliação `Restore`, 6 achados e a auditoria `ASSESSMENT_CREATED`, `ANALYSIS_IMPORTED`, `REPORT_GENERATED` |
| CI remota [run 30719625732](https://github.com/ricartefelipe/archlens/actions/runs/30719625732) | Evidência histórica do commit `c68450c`. **Não** reexecutada. O working tree ainda não está nesse remoto |
| OIDC com token assinado na API | Não homologado. Keycloak 26.0.7 subiu; o client `archlens-api` é bearer-only e o password grant respondeu `unauthorized_client`. O processo `%prod` aceitou a config segura só com `quarkus.oidc.tenant-enabled=false`, o que não valida JWT |
| Scanner SPDX / OWASP Dependency-Check / Trivy | Não executados |

## Operação

Instalação, perfis, backup, exclusão e problemas comuns: `docs/RUNBOOK.md` e `docs/ENVIRONMENTS.md`.

Upgrade de banco: o changeset `003-import-hash-and-blob-cleanup` cria o índice único de hash, a tabela de alias de idempotência, a fila de blob e torna `report_snapshot.assessment_id` anulável com `ON DELETE SET NULL`. Em base que já tenha duas importações iguais do mesmo hash na mesma avaliação, o changeset conserva a mais antiga.

## Backlog adiado

| Item | Classe | Por que adiar |
|------|--------|----------------|
| Baseline por equipe, supressão com validade, tendência | P2 | Exige histórico de várias análises reais antes de calibrar |
| CLI de pipeline do cliente e SARIF | P2 | O CLI atual já faz `scan`/`validate`; o formato extra espera um consumidor |
| Template de relatório por segmento, marca e tradução | P2 | Um relatório em pt-BR cobre o piloto |
| Conector Git somente leitura e análise incremental | P2 | O scan local por diretório já entrega o piloto offline |
| Catálogo de regras calibrado em projetos reais | P2 | O conjunto 1.0.0 existe; calibrar sem amostra real piora o score |
| Microsserviços ou Kubernetes obrigatório | P3 | A instalação é um processo Quarkus e um CLI |
| Correção automática de código | P3 | Contradiz a revisão humana |
| Modelo próprio ou vetores para parecer “IA” | P3 | A narrativa opcional já tem fallback determinístico |
| Marketplace de plugins | P3 | O contrato de análise ainda é o schema 1.0.0 |
| i18n, SSO corporativo além do OIDC já previsto, multi-região, sharding | P2/P3 | A oferta escolhida é instalação local ou serviço assistido, não SaaS |

Interoperabilidade com archlens-ai: os produtos continuam separados. O contrato compartilhado, se um dia existir, é o `analysis.schema.json` versão `1.0.0`. Nenhum código foi movido.

## Decisões do proprietário

- Adotar ou reescrever `docs/COMMERCIAL_TERMS_PROPOSAL.md` e escolher a licença.
- Credenciais reais de OIDC, banco, CORS e TLS para o primeiro piloto `%prod`.
- Homologar o piloto com um repositório do cliente, fora desta revisão.
- Commit, PR para `develop`, release e tags. Esta sessão não fez nenhuma dessas operações.


## Revisão de segurança e fechamento adicional — 22/10/2026, sessão direta

Esta seção atualiza as evidências acima, sem substituí-las retroativamente. As mudanças permanecem **somente no working tree** de `feature/melhorias-archlens`; o proprietário é responsável por staging, commits, push e PR.

### Correções aplicadas

- **OIDC desabilitado em produção:** `SecurityConfigValidator` passou a recusar `quarkus.oidc.tenant-enabled=false` quando o hardening está ativo. O perfil `%prod` estabelece explicitamente `quarkus.oidc.tenant-enabled=true`. Foi acrescentada regressão em `ProductionHardeningTest`.
- **Audiência dos tokens de demonstração:** `infra/keycloak/realm-archlens.json` agora configura `oidc-audience-mapper` no cliente `archlens-frontend`, incluindo `archlens-api` no access token. Esse realm é somente para desenvolvimento; deve ser recriado/importado para que a mudança tenha efeito em uma instância já existente.
- **Sessões prolongadas:** o frontend agora renova o access token por `refresh_token` no servidor, aceita rotação de refresh token, aplica timeout e não envia o refresh token ao objeto público `session`. Se a renovação falhar, o access token antigo deixa de ser usado. O uso de headers de identidade local só é permitido para sessões de login pelo provedor `local`, impedindo fallback de uma sessão OIDC para headers.
- **Teste E2E parametrizado:** `frontend/e2e/oidc.spec.ts` cobre o login Keycloak e a listagem da API, além de chamadas sem token e com headers forjados. Só executa com `ARCHLENS_E2E_OIDC=1` e credenciais sintéticas fornecidas por `OIDC_E2E_USERNAME` e `OIDC_E2E_PASSWORD`. Não armazene essas credenciais no repositório.

### Evidências de execução

- Backend: `./mvnw -B test` executou **41 testes, sem falhas nem ignorados**; `./mvnw -B package -DskipTests` compilou o artefato Quarkus após o endurecimento do tenant.
- Backend iniciado em perfil `demo` com `ARCHLENS_OIDC_ENABLED=true` e `ARCHLENS_LOCAL_HEADER_AUTH=false`, usando Keycloak 26.0.7 e Postgres 16 em projeto Compose **isolado** `archlens-oidc-validation`. `GET /api/v1/assessments` sem token respondeu **401**.
- **Não homologado:** o login Authorization Code do navegador com um access token assinado aceito pela API não foi concluído nesta sessão. A execução automatizada que acessava credenciais de teste e tokens foi bloqueada pelo mecanismo de segurança do acesso remoto; não foi contornado. O teste parametrizado acima permanece pendente de execução autorizada. A integração com o IdP real do cliente e CI remoto também permanecem pendentes.
- O ambiente Compose de validação foi desligado usando `down` **sem `-v`**. Seu volume de testes foi preservado; o projeto habitual não foi alterado.

### Dependências npm

A auditoria inicial encontrou **19 avisos totais** (3 críticos, 12 altos) e **8 avisos na árvore de produção** (1 crítico, 4 altos). Após atualizações seletivas das dependências transitivas e reinstalação com lockfile, o `npm audit --omit=dev` passou a **2 avisos baixos**, sem crítico/alto/moderado de produção. O `npm audit` total reportou **13 avisos** (2 críticos, 8 altos, 1 moderado, 2 baixos), associados sobretudo às ferramentas de desenvolvimento (incluindo Vitest/tinypool e ESLint). Não foram realizados upgrades principais de Vitest, Next ou Mermaid. O `package-lock.json` foi atualizado; `npm ci --ignore-scripts` concluiu com sucesso, usando autorização pontual de download npm, sem alterar a configuração persistente e sem executar scripts de instalação.

As versões e advisories remanescentes devem ser reavaliados antes de aceitar PRs não confiáveis no CI. Um resultado de audit não substitui inventário de runtime, scanner Java/container ou homologação de produção.

### Critérios ainda abertos

1. Executar e aprovar `frontend/e2e/oidc.spec.ts` com o fluxo Authorization Code e tokens válidos, mantendo o tenant OIDC ativo.
2. Executar GitHub Actions no commit efetivamente publicado, após o proprietário revisar e versionar todos os arquivos não rastreados.
3. Triar advisories de desenvolvimento e executar scanner de dependências Java e imagens.
4. Validar configurações de TLS, IdP, banco e credenciais reais no ambiente do primeiro cliente; escolher licença/termos comerciais.

**Veredito:** melhorias e endurecimento local aplicados. **Não homologado para implantação comercial corporativa até a autenticação OIDC positiva e os gates externos restantes serem comprovados.**
