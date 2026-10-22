# Limitações do analyzer (MVP)

## Geral

- Análise **somente estática** e somente leitura; nunca executa Maven/Gradle/JVM do alvo.
- Não envia código-fonte para rede, LLM ou APIs externas.
- Detecção de sinais usa **heurísticas regex/imports/caminhos**, não AST completo (exceto parsing textual leve de Java).

## MavenPomParser

- Resolve apenas propriedades **locais** do próprio POM; não baixa parent/BOM da rede.
- Versões `${...}` sem valor local são preservadas literalmente (não inventadas).
- `dependencyManagement` é inventariado à parte e não vira dependência efetivamente declarada.

## GradleBuildParser

- Não avalia Groovy/Kotlin; não resolve version catalogs (`libs.*` permanece não resolvido).
- Confiança tipicamente `MEDIUM`; cai para `LOW` com propriedades/`libs.*`.
- Strings enganosas e comentários são filtrados heuristicamente — falsos positivos/negativos possíveis.

## SafeFileWalker

- Não segue symlinks cujo alvo real está fora da raiz.
- Não reentra em diretório já visitado (ciclo de symlink).
- `--exclude` ignora caminho relativo ou nome de diretório.
- Ignora binários por extensão/conteúdo e diretórios padrão (`.git`, `target`, `build`, etc.).
- Limites default: 2 MiB/arquivo, 50k arquivos, 200 MiB totais. Excesso vira sinal `walk.diagnostic.*`.
- Erro de leitura vira diagnóstico; o arquivo não entra no inventário.

## Segredos

- Placeholders e variáveis de ambiente óbvias são ignorados; ainda pode haver falso positivo/negativo.
- O indicador de ocorrência não permite correlacionar o mesmo valor entre caminhos distintos só pelo hash nu.
- Arquivos `.sh`, `.bash`, `.conf` e `.cfg` são lidos como texto para detecção. Não são executados.

## Relatório

- Seção de detecção automatizada (certeza STRUCTURAL ou HEURISTIC) é distinta da revisão do arquiteto.
- PDF usa DejaVu Sans para acentos. O selo de integridade é o hash do HTML, não um hash separado do PDF.

## Frontend / auth / Compose

- No Compose **demo** o backend usa `QUARKUS_PROFILE=demo` com `ARCHLENS_LOCAL_HEADER_AUTH=true` (headers `X-User`/`X-Roles`). Isso é explícito e **não** é produção.
- Perfil `%prod` exige OIDC, rejeita header auth e falha no startup se secrets/CORS/TLS estiverem inseguros.
- Login local só com `AUTH_LOCAL_ENABLED=true`, senhas no ambiente e, para as senhas de demo, `ARCHLENS_ALLOW_DEMO_CREDENTIALS=true`. `ARCHLENS_PROFILE_LABEL=production` desliga o login local.
- OIDC Bearer ponta a ponta no Compose ainda não é o caminho padrão (implementado no backend prod; não totalmente testado E2E).
- Server actions no container precisam de `ARCHLENS_API_INTERNAL_URL=http://backend:8080/api/v1`; o browser continua em `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1`. Sem a URL interna, a UI mostra `fetch failed`.
- E2E contra o frontend Compose deve usar `PLAYWRIGHT_BASE_URL=http://localhost:3000` (não `127.0.0.1`) para alinhar cookies com `NEXTAUTH_URL`.
- Filtros de findings (status/severidade/busca) são client-side após GET completo.
- Downloads HTML/PDF/CSV via server actions; schema do `analysis.json` no servidor.
- Rate limit de import é in-memory (não compartilhado entre réplicas).
- Em restart agressivo, Liquibase pode ficar com changelog lock; liberar com `UPDATE databasechangeloglock SET locked=false...`.
- MVP é **demonstrável** em ambiente local/piloto assistido; não é SaaS público nem produção endurecida sem overlay/`%prod` real.
