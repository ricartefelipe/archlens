# Threat Model — ArchLens MVP

Atualizado em 2026-07-17 (Prompt 4/5 — hardening).

## Ativos

| Ativo | Sensibilidade |
|-------|---------------|
| Código-fonte analisado (local no CLI) | Alta |
| `analysis.json` (metadados/evidências sem código) | Média |
| Achados, scores, roadmap, relatórios | Média–Alta |
| Credenciais Keycloak / tokens / DB | Alta |
| Snapshots publicados | Média |
| `raw_json` de import (analysis.json) | Média |

## Superfície de ataque

1. **CLI analyzer** — leitura de filesystem do alvo
2. **API de importação** — upload de JSON
3. **OIDC / sessões / headers locais (só demo)** — frontend e backend
4. **Postgres** — dados da organização
5. **Publicação de relatório** — HTML/PDF baixáveis
6. **Swagger/OpenAPI** — superfície de descoberta (desligada em prod)

## Ameaças e mitigações

| Ameaça | Severidade | Mitigação |
|--------|------------|-----------|
| Upload malicioso / JSON gigante | Alta | JSON Schema; `max-body-size`; `archlens.import.max-bytes`; rate limit de import |
| ZIP bomb | Média | Analyzer **não** extrai ZIP de upload; ignora binários `.zip` no walk; N/A na API (só JSON) |
| XXE via POM | Alta | `MavenPomParser` com DTD/entidades externas desabilitadas |
| Symlink escape ou ciclo no scan | Alta | Não segue alvo fora da raiz; diretório já visitado é ignorado |
| Exclusão de caminho | Média | `scan --exclude` não lê o caminho pedido |
| Adulteração do `analysis.json` | Alta | `contentHash` recalculado; HTTP 400 |
| Vazamento de segredo no JSON/logs | Alta | Sem valor; hash contextual; CI bloqueia vazamento do segredo fictício; logs sem payload |
| Elevação de papel | Alta | RBAC no backend; prod sem headers `X-User`/`X-Roles`; login local desligado com perfil `production` |
| Acesso cruzado entre orgs | Alta | Filtro por `organization_id`; 404 em acesso cruzado, inclusive na exclusão |
| Exfiltração por LLM | Alta | `LLM_ENABLED=false` default; adapter não envia código/segredos |
| Replay / duplo import | Média | `Idempotency-Key` + contentHash conferido |
| Tampering de relatório | Média | Snapshot imutável; download confere hash do HTML (409 se divergir) |
| Credencial de demo em instalação de cliente | Alta | Sem senha padrão no código; flag explícita só no Compose demo |
| Dado retido após o contrato | Alta | `DELETE` da avaliação por ADMIN, com auditoria redigida |
| CSRF / clickjacking | Média | Headers `X-Frame-Options`, CSP `frame-ancestors` |
| Swagger público em prod | Média | OpenAPI/Swagger desabilitados no perfil `%prod` |
| Secrets em Compose | Média | Defaults só no perfil demo; prod exige env sem defaults |

## Premissas de confiança

- Operador confia no host onde roda o CLI
- Compose **demo** não é hardening de produção
- Uma organização por instalação (MVP)
- Usuários Keycloak do realm importado são **exclusivamente fictícios**

## Residual / aceito no MVP

- Autorização fine-grained por avaliação (pós-MVP)
- Assinatura criptográfica de snapshots além de hash (pós-MVP)
- Rate limit in-memory (não distribuído) — aceito para demo; produção deve usar gateway
- OIDC Bearer ponta a ponta no Compose demo ainda é opcional (headers locais no backend demo)
- Tracing distribuído planejado, não embutido

## Classificação de maturidade (capacidades)

| Capacidade | Estado |
|------------|--------|
| Analyzer offline + schema | Implementada e testada |
| Import + regras + relatório | Implementada e testada |
| RBAC / isolamento org | Implementada e testada (headers em demo/test) |
| OIDC backend endurecido | Implementada; **não totalmente testada** E2E com Bearer real |
| Métricas / correlationId | Implementada; validação operacional parcial |
| Rate limit import | Implementada (in-memory) |
| Detecção Gradle | Heurística |
| LLM narrativa | Opcional (desligada) |
| Multi-tenant SaaS | Planejada |
