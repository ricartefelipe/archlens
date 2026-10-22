# Segurança — ArchLens

Produto de **diagnóstico assistido** executado no ambiente do cliente. Não é SaaS público e não recebe código-fonte por padrão.

## O que não sai do perímetro

- O CLI lê o projeto **somente no host** em que é executado. Não abre rede, não invoca Maven/Gradle/JVM do alvo e não grava o texto-fonte no `analysis.json`.
- Segredos detectados viram tipo, caminho, linha e indicador contextual (`valueHash`). O valor não é persistido.
- O backend guarda o `analysis.json` (metadados, hashes, sinais), achados, questionário, roadmap e relatórios. Isso só deve existir na instalação do cliente.

## Controles implementados

| Controle | Onde |
|----------|------|
| Caminhamento sem seguir symlink externo, sem ciclo de diretório, com exclusão explícita (`--exclude`) | `SafeFileWalker`, `scan --exclude` |
| Limites de arquivo grande, binário e erro de leitura diagnosticado | `WalkLimits`, sinais `walk.diagnostic.*` |
| Parser Maven sem XXE | `MavenPomParser` |
| `contentHash` recalculado na importação; divergência → HTTP 400 | `ContentHashVerifier` |
| `schemaVersion` aceita apenas `1.0.0` (sem migração silenciosa) | schema + import |
| Snapshot de relatório com hash do HTML; adulteração → HTTP 409 | `ReportService` |
| RBAC ADMIN / ARCHITECT / VIEWER e isolamento por organização (404 cruzado) | API |
| Perfil `%prod` falha no startup se header auth, TLS trust-all, senha padrão, CORS `*` ou OIDC ausente | `SecurityConfigValidator` |
| Login local sem senha padrão no código; senhas de demo só com `ARCHLENS_ALLOW_DEMO_CREDENTIALS=true`; perfil `production` desliga login local | frontend |
| Exclusão administrativa da avaliação com redação da auditoria | `DELETE /api/v1/assessments/{id}` |
| Logs sem corpo do JSON; correlation id | filtro + `%prod` JSON |

## Segredos

- Não versionar `.env`, senhas de banco, client secrets ou tokens.
- O Compose padrão é **demonstração** e declara senhas fictícias de forma explícita. Não reutilize esse arquivo como instalação de cliente.
- Overlay de exemplo: `infra/docker-compose.prod.example.yml`. Secrets entram por ambiente, não pelo Git.

## TLS e identidade no cliente

OIDC/SSO corporativo é o caminho de instalação. O IdP, o certificado TLS e o audience são do cliente. O backend de produção exige issuer HTTPS (rejeita `localhost`), verificação TLS e papéis `ADMIN`, `ARCHITECT`, `VIEWER`.

Bearer OIDC de ponta a ponta **não** é o caminho do Compose demo e não foi homologado contra o IdP do cliente nesta sessão.

## Retenção e retirada

- `raw_json` existe para auditoria da importação, com limite de tamanho.
- ADMIN pode retirar os dados operacionais da avaliação (inventário, achados, questionário, scores, roadmap, HTML/PDF). A trilha de auditoria fica sem o conteúdo (`redigido na exclusão`) e registra `ASSESSMENT_ERASED`.
- Backup do Postgres é responsabilidade da instalação. O procedimento de dump/restore está em `OPERATIONS.md`. O script `scripts/backup-restore-smoke.sh` prova o caminho da ferramenta em banco efêmero, não o backup do cliente.

## O que ainda não está fechado

- Certificação, DPA/LGPD formal e pentest externo.
- Rate limit de import é em memória (uma instância).
- Hash do PDF não é selado à parte; o selo é o hash do HTML que o gerou.
- Multi-tenant em escala e alta disponibilidade não fazem parte da oferta.
