# Checklist de release — ArchLens

Use antes de entregar um build para piloto no cliente. Não publique release nem altere a visibilidade do repositório sem pedido explícito.

## Código e contratos

- [ ] Branch `feature/*` ou `fix/*` a partir de `develop` (ou `master` se for a branch estável do remoto).
- [ ] PR para a branch de integração. Sem merge direto na branch estável.
- [ ] `bash scripts/validate-contracts.sh` ok.
- [ ] `schemaVersion` continua `1.0.0` ou há ADR de migração e rejeição da versão anterior documentada.
- [ ] Nenhum segredo novo no Git (`bash scripts/scan-secrets.sh`).

## Testes

- [ ] `cd analyzer && ./mvnw -B test`
- [ ] `cd backend && DOCKER_HOST=unix:///var/run/docker.sock ./mvnw -B test` (Postgres real via Testcontainers)
- [ ] `cd frontend && npm run lint && npm run typecheck && npm test`
- [ ] E2E Playwright contra API real quando a stack demo estiver no ar (`ARCHLENS_E2E_REAL=1`)
- [ ] `bash scripts/backup-restore-smoke.sh` ou bloqueio explícito se não houver Docker

## Configuração do artefato de cliente

- [ ] Imagem/backend sobe com `QUARKUS_PROFILE=prod` e **falha** se secret, OIDC, CORS ou TLS estiverem inseguros.
- [ ] Frontend com `AUTH_LOCAL_ENABLED=false`, `ARCHLENS_ALLOW_DEMO_CREDENTIALS=false`, `ARCHLENS_PROFILE_LABEL=production`.
- [ ] Sem senha `admin123` / `architect123` / `viewer123` no ambiente do cliente.
- [ ] `ARCHLENS_LLM_ENABLED=false` salvo decisão escrita.
- [ ] TLS na borda e IdP HTTPS do cliente definidos. Sem isso, não marque homologado.

## Entrega do diagnóstico

- [ ] Scan feito com a fixture legal ou com o código no host do cliente, com `--exclude` do que ficou de fora.
- [ ] `analysis.json` validado; valor secreto não aparece em claro.
- [ ] Achados distinguem detecção automatizada (STRUCTURAL/HEURISTIC) e revisão do arquiteto.
- [ ] Relatório HTML/PDF com acentos legíveis e snapshot publicado (segundo publish recusado).
- [ ] Caminho de retirada (`DELETE` da avaliação) demonstrado ou agendado no offboarding.
- [ ] Termo de premissas assinado: `docs/commercial/PREMISSAS_E_ESCOPO.md`.

## Não afirmar

- [ ] Precisão absoluta do scanner.
- [ ] SaaS público, HA ou conformidade certificada.
- [ ] OIDC do cliente homologado, se o teste foi só com header auth de demo/teste.
