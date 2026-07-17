# Configuração por ambiente — ArchLens

## Perfis

| Perfil | Como ativar | Auth | Secrets | Uso |
|--------|-------------|------|---------|-----|
| **development** | `quarkusDev` / `%dev` | Headers locais | defaults locais | Bootstrap rápido (H2) |
| **test** | Surefire / `%test` | Headers locais | Testcontainers | CI e IT |
| **demo** | `QUARKUS_PROFILE=demo` (Compose) | Headers locais (OIDC UI opcional) | fictícios documentados | Demonstração local |
| **production** | `QUARKUS_PROFILE=prod` | OIDC obrigatório | **sem defaults utilizáveis** | Deploy real (pós-validação Prompt 5) |

O Compose padrão usa **demo**, não produção. Overlay de exemplo: `infra/docker-compose.prod.example.yml`.

## Variáveis críticas (backend)

| Variável | Demo | Produção |
|----------|------|----------|
| `QUARKUS_PROFILE` | `demo` | `prod` |
| `ARCHLENS_LOCAL_HEADER_AUTH` | `true` | `false` (forçado) |
| `ARCHLENS_OIDC_ENABLED` | `false` | `true` (forçado) |
| `ARCHLENS_OIDC_URL` / `ARCHLENS_OIDC_ISSUER` | localhost Keycloak | IdP real HTTPS |
| `ARCHLENS_OIDC_CLIENT_SECRET` | fictício | secret real |
| `ARCHLENS_OIDC_TLS_TRUST_ALL` | `true` | `false` |
| `ARCHLENS_DB_*` | `archlens` | secret real |
| `ARCHLENS_CORS_ORIGINS` | `http://localhost:3000` | origem HTTPS da app |
| `ARCHLENS_HSTS_ENABLED` | `false` | `true` sob HTTPS |
| `ARCHLENS_OPENAPI_ENABLED` | Swagger UI no demo | `false` |
| `ARCHLENS_LLM_ENABLED` | `false` | `false` salvo necessidade explícita |

## Variáveis críticas (frontend)

| Variável | Demo | Produção |
|----------|------|----------|
| `AUTH_LOCAL_ENABLED` | `true` | `false` |
| `AUTH_SECRET` / `NEXTAUTH_SECRET` | local | secret forte (≥32 chars) |
| `KEYCLOAK_*` | localhost | IdP real |
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080/api/v1` | URL pública da API (browser) |
| `ARCHLENS_API_INTERNAL_URL` | `http://backend:8080/api/v1` (Compose) | URL server-side no container; omitir no `npm run dev` local |

## Endpoints de observabilidade

| Endpoint | Significado |
|----------|-------------|
| `/q/health/live` | Processo vivo |
| `/q/health/ready` | Pronto (DB + regras) |
| `/q/metrics` | Prometheus (import, schema fail, findings, relatório, durações) |
| `/api/v1/health` | Status app + `profile` + `localHeaderAuth` |

## Política `analysis.json` / `raw_json`

- Apenas metadados do analyzer (paths, sinais, hashes) — **nunca código-fonte**.
- Limite: `ARCHLENS_IMPORT_MAX_BYTES` (default 5 MiB).
- Retenção: auditoria de import (`archlens.raw-json.retention=import-audit`).
- Logs não devem dumpar o JSON completo nem tokens.

## Analyzer offline

O CLI não precisa de rede, Docker ou Keycloak. Roda no host do código legado e produz `analysis.json` validável offline.
