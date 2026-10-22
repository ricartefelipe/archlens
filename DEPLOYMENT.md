# Implantação — ArchLens

Instalação **no perímetro do cliente**, uma organização por ambiente. Não publique este Compose demo na internet.

## O que implantar

| Peça | Onde roda | Rede |
|------|-----------|------|
| CLI analyzer | Máquina com o código legado, somente leitura | Nenhuma |
| Backend Quarkus + Postgres | Rede interna do cliente | API só para o frontend |
| Frontend Next.js | Rede interna do cliente | Browser dos arquitetos |
| IdP OIDC (Keycloak do cliente ou corporativo) | Já existente | HTTPS |

O `analysis.json` é o único artefato que atravessa da máquina de scan para a API, e só se o contrato permitir.

## Demo local (não é entrega)

```bash
export DOCKER_HOST=unix:///var/run/docker.sock
docker compose -f infra/docker-compose.yml up --build -d
curl -fsS http://localhost:8080/q/health/ready
```

Esse arquivo liga header auth, senhas fictícias e `ARCHLENS_ALLOW_DEMO_CREDENTIALS=true`. Serve para a demonstração com `samples/legacy-java-demo`.

## Instalação de cliente

1. Copie `infra/docker-compose.prod.example.yml` para um arquivo **fora do Git**.
2. Injete `ARCHLENS_DB_PASSWORD`, `ARCHLENS_OIDC_CLIENT_SECRET`, `KEYCLOAK_CLIENT_SECRET` e `AUTH_SECRET` (≥ 32 caracteres) pelo secret store do cliente.
3. Aponte `ARCHLENS_OIDC_URL` e `ARCHLENS_OIDC_ISSUER` para o IdP HTTPS real. `localhost` faz o backend recusar a subida.
4. Restrinja `ARCHLENS_CORS_ORIGINS` à origem HTTPS da aplicação. Sem `*`.
5. Mantenha `QUARKUS_PROFILE=prod`, `ARCHLENS_LOCAL_HEADER_AUTH=false`, `ARCHLENS_OIDC_TLS_TRUST_ALL=false`, `AUTH_LOCAL_ENABLED=false`, `ARCHLENS_ALLOW_DEMO_CREDENTIALS=false`, `ARCHLENS_PROFILE_LABEL=production`.
6. Termine TLS no proxy do cliente (certificado do cliente). O backend exige verificação TLS ao falar com o IdP. HSTS fica ligado com `ARCHLENS_HSTS_ENABLED=true` quando a borda é HTTPS.
7. Confirme que o processo **não sobe** se algum item acima estiver inseguro (`SecurityConfigValidator`).
8. Rode o CLI no host do código. Importe só o JSON validado (`archlens validate`).

Detalhe de variáveis: `docs/ENVIRONMENTS.md`.

## O que esta sessão não fez

- Não houve deploy em nuvem, alteração de visibilidade do repositório, release publicada nem pagamento.
- Certificado TLS, backup agendado e IdP real dependem do cliente. Sem isso a instalação não está homologada.
- Não há Kubernetes nem segunda réplica. Uma instância atende o piloto.

## Migrações

Liquibase aplica o changelog no start. O changeset `003-finding-origin-certainty` tem rollback SQL (`DROP COLUMN`). Não edite changesets já aplicados; adicione outro.

Schema de análise `1.0.0` é o único aceito. Uma versão futura exige CLI e backend novos ao mesmo tempo. Payload `1.0.0` adulterado é recusado; não é reescrito.
