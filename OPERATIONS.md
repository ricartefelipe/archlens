# Operação — ArchLens

Complementa o [runbook](docs/RUNBOOK.md) (subir e parar a stack). Aqui está o que o operador faz durante um diagnóstico no cliente.

## Sinais de saúde

| Probe | Uso |
|-------|-----|
| `GET /q/health/live` | Processo vivo |
| `GET /q/health/ready` | Banco e regras carregadas. É a dependência do Compose |
| `GET /q/metrics` | Prometheus. Ver `docs/OBSERVABILITY.md` |
| `GET /api/v1/health` | Perfil efetivo e se header auth está ligado |

Em produção, header auth ligado ou perfil diferente de `production` é incidente: a instalação não é a combinada.

Logs de produção são JSON e carregam `correlationId`. Não registre corpo de `analysis.json`, token ou senha.

## Backup e restauração

No ambiente do cliente, o dado está no volume Postgres. Procedimento esperado (ajuste usuário, banco e destino fora do Git):

```bash
docker exec <postgres> pg_dump -U archlens -d archlens -Fc -f /tmp/archlens.dump
docker cp <postgres>:/tmp/archlens.dump ./archlens.dump
# restauração em instância de teste, nunca por cima da produção sem janela
docker exec -i <postgres> pg_restore -U archlens -d archlens --clean --if-exists /tmp/archlens.dump
```

Prova do caminho da ferramenta, com banco efêmero e sem dado de cliente:

```bash
export DOCKER_HOST=unix:///var/run/docker.sock
bash scripts/backup-restore-smoke.sh
```

Exit `2` significa Docker indisponível: o procedimento **não** foi executado. Exit `0` prova que uma linha inserida sobrevive a dump, drop e restore. Isso não substitui o teste de restauração no backup real do cliente, que continua pendente de homologação.

`docker compose down` sem `-v` preserva o volume. `-v` apaga o diagnóstico.

## Retirada de dados (offboarding)

Somente `ADMIN`:

`DELETE /api/v1/assessments/{id}`

Remove import (`raw_json`), inventário, achados, questionário, scores, opções, roadmap e snapshots HTML/PDF daquela avaliação na organização do chamador. Outra organização recebe 404. ARCHITECT e VIEWER recebem 403.

A auditoria anterior da avaliação é redigida (`redigido na exclusão`). Um evento `ASSESSMENT_ERASED` fica sem o nome do projeto e sem o JSON. Na interface, o botão "Retirar dados da avaliação" aparece só para ADMIN.

## Incidente curto

1. Anote `correlationId` do Problem Details.
2. Confira `/q/health/ready` e o log JSON do mesmo id.
3. Importação 400 com `contentHash`: o JSON foi alterado depois do `scan`. Gere de novo no CLI; não reescreva o hash à mão.
4. Download de relatório 409 `adulterado`: o HTML persistido não confere com o hash. Não entregue esse arquivo. Gere outro snapshot se a avaliação ainda existir.
5. Startup em prod recusado: leia a mensagem do `SecurityConfigValidator` e corrija secret, issuer, CORS ou TLS. Não desligue `ARCHLENS_REQUIRE_HARDENED`.
6. Lock do Liquibase após restart agressivo: ver runbook. Não apague o changelog.

## Limites de recurso

- Import padrão: 5 MiB (`ARCHLENS_IMPORT_MAX_BYTES`).
- Rate limit de import: 30/minuto por IP, em memória, uma instância.
- CLI: 2 MiB por arquivo, 50 mil arquivos, 200 MiB no total. Excedente vira diagnóstico, não leitura parcial silenciosa de arquivo acima do limite.

## Suporte de piloto

Horário, canal e tempo de resposta são combinados no termo de premissas (`docs/commercial/PREMISSAS_E_ESCOPO.md`). Não há plantão implícito.
