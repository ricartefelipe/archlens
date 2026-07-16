# Threat Model — ArchLens MVP

## Ativos

| Ativo | Sensibilidade |
|-------|---------------|
| Código-fonte analisado (local no CLI) | Alta |
| `analysis.json` (metadados/evidências sem código) | Média |
| Achados, scores, roadmap, relatórios | Média–Alta |
| Credenciais Keycloak / tokens | Alta |
| Snapshots publicados | Média |

## Superfície de ataque

1. **CLI analyzer** — leitura de filesystem do alvo
2. **API de importação** — upload de JSON
3. **OIDC / sessões** — frontend e backend
4. **Postgres** — dados da organização
5. **Publicação de relatório** — HTML/PDF baixáveis

## Ameaças e mitigações

| Ameaça | Mitigação MVP |
|--------|----------------|
| Path traversal / symlink escape no scan | Resolver path canônico; bloquear saída da raiz; não seguir symlinks externos |
| Execução de código do alvo | Proibido: sem Maven/Gradle/JVM do projeto |
| Vazamento de segredos no JSON | Apenas tipo/path/linha/hash; nunca valor |
| JSON malicioso no import | Validação JSON Schema; limites de tamanho; rejeição 400 problem+json |
| Replay / duplo import | Idempotency-Key |
| Privilege escalation | RBAC ADMIN/ARCHITECT/VIEWER no backend |
| Exfiltração via LLM | `LLM_ENABLED=false`; porta não envia código/segredos |
| SSRF via LLM URL | Desligado por padrão; allowlist futura |
| Tampering de relatório | Snapshot imutável + hash de conteúdo |
| Logs com PII/segredos | Sem payloads de código; sem valores de secret |

## Premissas de confiança

- Operador confia no host onde roda o CLI
- Compose local não é hardening de produção
- Uma organização por instalação

## Residual

- Autorização fine-grained por avaliação fica para pós-MVP
- Assinatura criptográfica de snapshots (além de hash) fica para pós-MVP
