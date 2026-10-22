# Contrato de análise (`analysis.json`)

## Fonte canônica

- Schema: `contracts/analysis.schema.json`
- Cópia verificável no backend: `backend/src/main/resources/analysis.schema.json`
- O teste `SchemaSyncTest` falha se as cópias divergirem.

## Versões e metadados

| Campo | Papel |
|-------|--------|
| `schemaVersion` | Constante `1.0.0` |
| `analyzerVersion` | Versão do CLI/core |
| `generatedAt` | Timestamp ISO-8601 (volátil) |
| `durationMs` | Duração da análise (volátil) |
| `contentHash` | `sha256:` + hex do payload canônico |
| `metrics` | Estatísticas do inventário |

## Conteúdo canônico (hash)

Algoritmo versionado: `archlens-content-hash/v1` (analyzer e backend usam a mesma regra).

O `contentHash` é calculado sobre o JSON serializado com propriedades ordenadas, **excluindo**:

- `generatedAt`
- `durationMs`
- `contentHash`

Duas análises do mesmo conteúdo de projeto (mesmos arquivos relevantes) devem produzir o mesmo `contentHash`.

Na importação o backend **recalcula** o hash e rejeita divergência com o valor declarado. A unicidade é `(organização, avaliação, contentHash)`: o mesmo artefato não gera outra importação; outra avaliação ou outra organização não reaproveita a linha. SHA-256 aqui confere integridade do JSON contra alteração acidental ou troca do campo `contentHash`. Não autentica a origem: quem consegue alterar o arquivo também consegue recalcular o hash.

Coleções são ordenadas antes da serialização (sinais, deps, evidências, etc.).

## Segredos e evidências

- Segredos: `type`, `path`, `line`, `detector`, `confidence`, `valueHash`
- `valueHash` é indicador contextual (`archlens/secret-indicator/v1` + tipo + path + linha + valor), **não** SHA-256 nu do segredo
- Evidências nunca armazenam trecho de código nem `matcher.group()` com valor secreto
- Validação rejeita JSON inválido com mensagem segura (sem dump do arquivo analisado)

## CLI

| Código | Significado |
|--------|-------------|
| 0 | Sucesso |
| 1 | Erro interno |
| 2 | Entrada inválida |
| 3 | Falha de validação de schema |
