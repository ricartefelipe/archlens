# ArchLens — Prontidão comercial (diagnóstico assistido)

Atualizado em **2026-10-22**. Posicionamento para piloto no ambiente do cliente. Sem preço de mercado, sem cliente citado e sem promessa de precisão absoluta.

O pacote entregue está em [PACOTE_DIAGNOSTICO](commercial/PACOTE_DIAGNOSTICO.md). O anexo de premissas está em [PREMISSAS_E_ESCOPO](commercial/PREMISSAS_E_ESCOPO.md).

## Cliente ideal, comprador e usuário

- **Cliente ideal:** empresa com Java legado (Maven/Gradle) pouco documentado, que não pode enviar código para fora.
- **Comprador:** patrocinador da modernização (liderança de arquitetura ou tecnologia).
- **Usuário:** arquiteto que roda o CLI no host do cliente e revisa achados.

## Pitch

O time precisa de inventário, riscos e um plano de 30/60/90 dias sem entregar o código a um serviço externo. O ArchLens lê o projeto no perímetro, importa só metadados e hashes, e o arquiteto publica um relatório em que dá para ver o que foi detecção automática e o que foi decisão humana.

## Oferta de piloto

Um sistema, uma instalação no cliente, fee fixo a combinar. Entrega: inventário, achados revisados, riscos, esforço estimado pelo arquiteto, roadmap, reunião de apresentação e termo de escopo. A demonstração de 5–10 minutos usa apenas `samples/legacy-java-demo` ([roteiro](DEMO_SCRIPT.md)).

**Preço:** hipótese de empacotamento (piloto fechado), não número de mercado. O valor só depois de conversas reais. Nada neste repositório é proposta comercial.

**Suporte:** hipótese a negociar no anexo de premissas (horário comercial, sem plantão). Não é SLA vigente.

## O que o piloto não é

SaaS público, scanner infalível, pentest, projeto de implementação da modernização, ou ambiente Compose demo com senha `admin123`.

## Responsabilidades

| Fornecedor | Cliente |
|------------|---------|
| Análise offline, importação, revisão assistida, relatório | Host, exclusões, IdP, TLS, backup |
| Dizer o que é heurístico | Arquiteto que aceita ou dispensa achados |
| Retirar dados quando pedido | Revogar acessos no encerramento |

## Automatizado vs revisão humana

| Automatizado | Revisão humana obrigatória |
|--------------|----------------------------|
| Walk seguro, inventário, sinais | Aceite do achado |
| Certeza STRUCTURAL (construto visto) ou HEURISTIC (regex/métrica) | Interpretação de risco |
| Segredo só por indicador, nunca o valor | Decidir se o risco é real |
| Scores por dimensão com rationale | Não tratar como nota única |
| Relatório com as duas seções e snapshot | Texto entregue ao patrocinador |

## Antes de analisar código de cliente

1. NDA ou o anexo de premissas assinado.
2. Host de leitura e lista `--exclude`.
3. Confirmação de que só `analysis.json` sai da máquina de scan, se sair.
4. Instalação com OIDC real, sem login local e sem senha de demo.
5. Backup do Postgres combinado e teste de restauração no ambiente do cliente (o smoke do repositório não conta como esse teste).
6. Quem é ADMIN para a retirada no fim.

## Riscos que continuam bloqueando SaaS público

Multi-tenant em escala, OIDC do cliente ainda não homologado nesta sessão, compliance externa, rate limit em memória, expectativa de precisão absoluta e qualquer upload de fonte para a nuvem do fornecedor.

## Veredito de venda

**GO PARA PILOTO** assistido, no perímetro do cliente, com o termo de premissas. **NO-GO** para SaaS público e para declarar produção homologada sem IdP, TLS e restauração reais do cliente.
