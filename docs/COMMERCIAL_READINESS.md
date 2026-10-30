# ArchLens — Prontidão comercial (serviço diagnóstico assistido)

Atualizado em **2026-07-17**. Documento de posicionamento — **sem preço** e **sem promessa de precisão absoluta**.

## Cliente-alvo

- Empresas com aplicações **Java legadas** (Maven/Gradle) pouco documentadas.
- Times de arquitetura / modernização que precisam de inventário, riscos e plano executável.
- Cenários em que **código-fonte não pode sair** do perímetro do cliente (ou do ambiente de consultoria controlado).

## Problema que endereçamos

Sistemas legados com conhecimento concentrado, dívida técnica opaca e pressão por modernização — sem tempo para análise manual completa e sem aceitar upload de fonte para SaaS genérico.

## Oferta: serviço de diagnóstico assistido

ArchLens é a **plataforma de apoio** a um engajamento de arquitetura:

1. Análise estática **offline** no ambiente do cliente (CLI).
2. Importação apenas do `analysis.json` (metadados/hashes/sinais — sem fonte).
3. Regras + questionário + scores com rationale.
4. Revisão humana de findings.
5. Roadmap e relatório HTML/PDF publicável (snapshot imutável).
6. Auditoria da jornada.

Não é substituir o arquiteto; é **acelerar e rastrear** o diagnóstico.

## Entradas

| Entrada | Responsável | Nota |
|---------|-------------|------|
| Código Java legado (leitura local) | Cliente / consultoria no site do cliente | Nunca enviado ao backend na jornada padrão |
| `analysis.json` gerado pelo CLI | Operador técnico | Validado contra schema |
| Respostas de questionário | Arquiteto | Contexto de negócio/infra |
| Revisões de findings / roadmap | Arquiteto | Decisão humana |

## Entregáveis

- Inventário (módulos, dependências, sinais).
- Findings com evidências e status de revisão.
- Scores dimensionais com rationale (assistidos).
- Opções/itens de modernização e roadmap.
- Relatório HTML e PDF; snapshot publicado imutável.
- Trilha de auditoria da avaliação.

## Responsabilidades do arquiteto

- Validar findings (aprovar/rejeitar/comentar).
- Interpretar scores no contexto do negócio.
- Priorizar roadmap e comunicação ao patrocinador.
- Garantir que o escopo do scan e limites do analyzer estão claros ao cliente.

## Automatizado vs revisão humana

| Automatizado | Revisão humana obrigatória |
|--------------|----------------------------|
| Walk seguro de arquivos, inventário, sinais/heurísticas | Aceitação de findings |
| Detecção de segredos **por hash** (não valor) | Interpretação de risco real |
| Regras → findings candidatos | Priorização e exclusões |
| Cálculo de scores/rationale assistido | Decisão de modernização |
| Geração de relatório/snapshot | Conteúdo final entregue ao cliente |

## Limites e disclaimers

- Análise **estática**; não executa o sistema alvo.
- Parsers e detectores são **heurísticos** — falsos positivos/negativos são esperados.
- Gradle/version catalogs e POMs com parents remotos têm resolução limitada.
- Demo Compose usa autenticação simplificada — **não** é o modo de entrega em cliente.
- Não há garantia de cobertura completa nem de “nota única” de qualidade.

## Requisitos antes de analisar código de cliente

1. Acordo de confidencialidade / escopo escrito.
2. Ambiente isolado (máquina ou rede do cliente) para o CLI.
3. Lista do que **não** será escaneado (secrets vaults, binários, dados pessoais).
4. Confirmação de que apenas `analysis.json` (ou artefatos equivalentes) sairá da máquina de scan, se houver transferência.
5. Perfis de acesso (ARCHITECT/VIEWER) e organização definidos.
6. Backup e política de retenção do Postgres combinados.
7. Treino rápido do arquiteto na jornada e nas limitações.

## Implantação isolada / offline

- **Recomendado para piloto:** Compose/demo endurecido ou overlay prod **no perímetro do cliente**, com OIDC real, secrets reais, TLS, CORS restrito.
- CLI analyzer: zero rede necessária.
- Backend/frontend: rede local; sem dependência de LLM por padrão (`ARCHLENS_LLM_ENABLED=false`).

## Riscos que bloqueiam SaaS público

| Risco | Por que bloqueia |
|-------|------------------|
| Multi-tenant forte + isolamento comprovado em escala | MVP isola por org em demo; não é produto multi-tenant maduro |
| OIDC/SSO corporativo ponta a ponta no caminho padrão | Demo ainda favorece headers/login local |
| Compliance (LGPD, retenção, DPA, auditoria externa) | Não certificado |
| Rate limit / HA / secrets management em cluster | Rate limit in-memory; sem K8s HA |
| Expectativa de precisão absoluta de scanner | Heurístico por design |
| Upload de fonte para nuvem do fornecedor | Contrário à proposta de valor |

## Posicionamento de venda (piloto)

Oferecer **piloto assistido** (semanas) com entrega de relatório e workshop de roadmap — não assinatura SaaS pública.  
Preço e SLA ficam fora deste documento até validação comercial separada.

## Modalidade desta revisão

Licença de uso instalada no perímetro do cliente, ou serviço de diagnóstico em que a ferramenta apoia o arquiteto. Não há checkout. A licença do código **não foi escolhida**; ver [COMMERCIAL_TERMS_PROPOSAL](COMMERCIAL_TERMS_PROPOSAL.md).

| Função | Estado real |
|--------|-------------|
| Scan offline Maven/Gradle sem executar o build | Implementado; Gradle e BOM/parent remotos têm limitação explícita |
| Importação com hash conferido e escopo por organização | Implementado |
| Questionário, achados, roadmap, relatório HTML/PDF | Implementado; achados são candidatos heurísticos |
| Login demo por headers | Só perfis local, demo, dev e test |
| Produção com OIDC e sem headers forjados | Configurado; não homologado com IdP real nesta revisão |
| Narrativa de provedor externo | Opt-in, timeout e texto mínimo; padrão é o template determinístico |
| Exclusão de avaliação e limpeza de PDF | Implementado; retenção legal é configuração, não política jurídica |
