# Changelog

## 2026-10-22

Preparação do diagnóstico assistido para piloto no ambiente do cliente. O Compose demo continua sendo demonstração, não instalação de produção.

### Segurança e integridade

- A importação recalcula o `contentHash` canônico e rejeita JSON adulterado (HTTP 400). Só a `schemaVersion` `1.0.0` é aceita.
- O snapshot de relatório confere o hash do HTML no download. HTML adulterado responde HTTP 409.
- O walker ignora ciclo de symlink e diretório já visitado, registra erro de leitura e honra `--exclude`.
- Scripts shell e arquivos de configuração entram na detecção de segredo sem serem executados. O valor não vai para o JSON.
- Login local não tem mais senha padrão no código. Senhas de demonstração exigem `ARCHLENS_ALLOW_DEMO_CREDENTIALS=true`. Perfil `production` desliga o login local.
- A imagem do frontend não liga mais `AUTH_LOCAL_ENABLED` durante o build.

### Produto

- Achados carregam origem `AUTOMATED` e certeza `STRUCTURAL` ou `HEURISTIC`.
- O relatório HTML/PDF separa detecção automatizada e revisão do arquiteto, explica que não há nota única e embute fonte para acentos.
- `DELETE /api/v1/assessments/{id}` (ADMIN) apaga os dados da avaliação, inclusive relatórios publicados, e redige a auditoria. ARCHITECT recebe 403; outra organização recebe 404.
- CSV de achados inclui certeza, origem e revisão.

### Operação

- `scripts/backup-restore-smoke.sh` prova dump/restore em Postgres efêmero.
- Documentos: `SECURITY.md`, `DEPLOYMENT.md`, `OPERATIONS.md`, `RELEASE_CHECKLIST.md`, pacote comercial em `docs/commercial/`.

### Fora deste corte

- Homologação OIDC com o IdP do cliente.
- Backup restaurado a partir do volume real de uma instalação.
- Certificação, pentest externo e preço de mercado.
