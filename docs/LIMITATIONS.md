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
- Ignora binários por extensão/conteúdo e diretórios padrão (`.git`, `target`, `build`, etc.).
- Limites default: 2 MiB/arquivo, 50k arquivos, 200 MiB totais.

## Segredos

- Placeholders e variáveis de ambiente óbvias são ignorados; ainda pode haver falso positivo/negativo.
- O indicador de ocorrência não permite correlacionar o mesmo valor entre caminhos distintos só pelo hash nu.

## Frontend / auth (Prompt 3)

- No Compose local o backend opera com `ARCHLENS_OIDC_ENABLED=false` e identidade via headers `X-User`/`X-Roles` propagados pela sessão NextAuth (login local ou claims OIDC). OIDC completo no backend ainda é pendência.
- Login local só com `AUTH_LOCAL_ENABLED=true`; não deve ser usado em produção.
- Filtros de findings (status/severidade/busca) são aplicados no cliente após o GET da lista completa.
- Downloads HTML/PDF/CSV passam por server actions (evitam CORS no browser); a validação de schema do `analysis.json` permanece no servidor.
- Em restart agressivo do backend, o Liquibase pode ficar com changelog lock; liberar com `UPDATE databasechangeloglock SET locked=false...` e subir um único container.
