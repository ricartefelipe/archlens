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

O `contentHash` é calculado sobre o JSON serializado com propriedades ordenadas, **excluindo**:

- `generatedAt`
- `durationMs`
- `contentHash`

Duas análises do mesmo conteúdo de projeto (mesmos arquivos relevantes) devem produzir o mesmo `contentHash`.

O backend **recalcula** esse hash na importação (`ContentHashVerifier`). Payload com hash divergente é recusado. Não se aceita "corrigir" o hash no cliente.

Coleções são ordenadas antes da serialização (sinais, deps, evidências, etc.).

## Versão

Somente `1.0.0`. Outra `schemaVersion` não é migrada no import: a API responde erro e o operador atualiza o CLI. Uma `1.1.0` futura precisa de ADR, schema novo e janela em que o backend ainda sabe recusar a versão que não implementa.

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
