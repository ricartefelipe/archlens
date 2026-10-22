# Inventário de licenças — para decisão do proprietário

Este arquivo **não escolhe** a licença do ArchLens e **não** é um parecer jurídico. Nenhum scanner de licenças SPDX foi executado sobre a árvore transitiva.

## Projeto

| Item | Estado em 22/10/2026 |
|------|----------------------|
| Arquivo `LICENSE` na raiz | Ausente |
| Declaração no README | Pendente de escolha do proprietário |
| Direitos concedidos por esta revisão | Nenhum |

## Dependências diretas observadas nos manifestos

Versões lidas dos arquivos do repositório, não de um relatório de licença.

| Origem | Componentes diretos |
|--------|---------------------|
| `analyzer/pom.xml` | Jackson 2.18.2, picocli 4.7.6, json-schema-validator 1.5.6, JUnit 5.11.4 |
| `backend/pom.xml` | Quarkus BOM 3.33.2, Testcontainers BOM 1.20.4 |
| `frontend/package.json` | Next.js 15.5.28, React 19.1.0, next-auth 5.0.0-beta.32, mermaid 11.16.1; dev: Playwright, Vitest 3, ESLint, Tailwind 4, TypeScript 5 |
| Imagens citadas em `docs/DECISIONS.md` | `postgres:16.6-alpine`, Keycloak `26.0.7`, Temurin 21, Node 22 |

Antes de distribuir, o proprietário precisa de um relatório de licenças transitivas (por exemplo o plugin de licenças do Maven e `license-checker` no frontend) e da compatibilidade com a licença que ele escolher.

## Vulnerabilidades

`npm audit` em 22/10/2026, depois de atualizar Next.js para 15.5.28, next-auth para 5.0.0-beta.32 e mermaid para 11.16.1, reportou **19** avisos (2 baixos, 2 moderados, 12 altos, 3 críticos). A contagem anterior, antes desse bump, era 21 (1 baixo, 3 moderados, 12 altos, 6 críticos). Não foi usado `npm audit fix --force`. Não foi executado OWASP Dependency-Check nem Trivy. Três scripts de instalação (esbuild, sharp, unrs-resolver) foram bloqueados pelo ambiente; o audit pode estar incompleto para binários nativos.

O que foi atualizado e por quê: Next 15.5.20 estava abaixo de 15.5.28, faixa em que o próprio advisory aponta DoS de Server Actions, SSRF e RCE na otimização de imagem (GHSA-2464-fhr4-w9xw, GHSA-p3m5-59mp-jv5r e os anteriores da série). next-auth 5.0.0-beta.31 estava na faixa `<=5.0.0-beta.31` de GHSA-8fpg-xm3f-6cx3, GHSA-7rqj-j65f-68wh, GHSA-xmf8-cvqr-rfgj e GHSA-x445-f3h2-j279. mermaid abaixo de 11.16.1 cobria GHSA-c4c3-pg64-4m4v e correlatos.

O que ficou, de propósito: Vitest 3 / tinypool (GHSA-5gmw-xhrv-c9v3, GHSA-85c8-ppgw-ccpr) só no teste, e a correção indicada é Vitest 5. DOMPurify `<=3.4.16` chega via mermaid (GHSA-cg2g-xh64-v4q7); a correção indicada pelo audit puxa mermaid 12. PostCSS e sharp continuam transitivos de Next; a correção indicada é Next 16. Nenhum desses saltos de major foi aplicado.

## Próximo passo do proprietário

1. Escolher a licença do código (ver proposta separada).
2. Rodar inventário SPDX da árvore completa e guardar o relatório fora do produto, ou versioná-lo se quiser que acompanhe o release.
3. Decidir quais avisos do `npm audit` entram no prazo de correção antes da primeira entrega a cliente.
