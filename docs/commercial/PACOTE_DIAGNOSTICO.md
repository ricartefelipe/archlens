# Pacote de diagnóstico — ArchLens

Oferta de **serviço assistido** no ambiente do cliente. A plataforma organiza o trabalho; o arquiteto responde pelo diagnóstico entregue. Não é scanner de precisão absoluta nem SaaS público.

Amostra legalmente compartilhável para demonstração: `samples/legacy-java-demo`. Código de cliente não entra em demo, repositório ou ambiente do fornecedor.

## Quem compra e quem usa

| Papel | Quem é | O que precisa |
|-------|--------|----------------|
| Cliente ideal | Empresa com aplicação Java (Maven ou Gradle) pouco documentada e pressão para modernizar, sem autorização para enviar código a um SaaS | Diagnóstico rastreável dentro do próprio perímetro |
| Comprador | Patrocinador da modernização (liderança de arquitetura ou tecnologia) | Inventário, riscos priorizados, esforço estimado e plano de 30/60/90 dias |
| Usuário | Arquiteto ou tech lead que opera o CLI e revisa achados | Jornada repetível e evidência do que foi automático e do que foi decidido |

Não há clientes, cases ou certificações para citar.

## Dor que o piloto mede

Antes: o time não consegue dizer, com evidência, quais módulos, dependências, sinais de legado e lacunas de teste existem, nem separar o que a ferramenta viu do que o arquiteto aceitou.

Depois do piloto, o patrocinador recebe um pacote com:

1. Inventário (módulos, pacotes, tipos, endpoints, persistência, mensageria, dependências, métricas).
2. Achados com severidade, certeza (`STRUCTURAL` ou `HEURISTIC`), evidência e status de revisão.
3. Riscos que o arquiteto aprovou, aceitou ou dispensou, com nota.
4. Esforço estimado nas opções de modernização (baixo/médio/alto — julgamento do arquiteto, não fórmula de mercado).
5. Roadmap 30/60/90 dias (fases de curto, médio e longo prazo no produto).
6. Relatório HTML e PDF publicado (snapshot imutável) e trilha de auditoria.
7. Reunião de apresentação de 45–60 minutos usando o relatório publicado.
8. Termo do que ficou de fora (`docs/commercial/PREMISSAS_E_ESCOPO.md`).

## Jornada

1. Cliente autoriza escopo, exclusões e host de leitura.
2. Operador roda `scan` offline, com `--exclude` do que não entra.
3. Valida o JSON. Confere que segredo em claro não está no arquivo e que o `contentHash` fecha.
4. Importa na instalação do cliente. O backend recusa payload adulterado.
5. Arquiteto responde o questionário, revisa achados e monta o roadmap.
6. Publica o relatório. Uma segunda publicação do mesmo snapshot é recusada.
7. Apresenta. No encerramento, ADMIN retira os dados se o contrato pedir.

## O que a ferramenta faz e o que o arquiteto faz

| Automatizado | Revisão humana |
|--------------|----------------|
| Inventário e sinais, com confiança HIGH/MEDIUM/LOW | Aceitar, recusar ou assumir o risco |
| `STRUCTURAL`: construto observado pelo parser | Traduzir isso em risco de negócio |
| `HEURISTIC`: regex, métrica ou dependência aproximada | Tratar falso positivo e falso negativo |
| Scores por dimensão, com rationale | Não usar uma nota única como veredito |
| Relatório e hash | Texto que o patrocinador pode levar |

## Esforço e roadmap

O produto não calcula horas. O arquiteto classifica cada opção como esforço baixo, médio ou alto e encaixa no roadmap:

- 30 dias: conter risco e tornar o sistema observável (curto prazo).
- 60 dias: quebrar acoplamento ou dependência crítica já evidenciada (médio prazo).
- 90 dias: primeiro incremento de modernização com dono e critério de pronto (longo prazo).

Itens sem evidência no inventário não entram como fato. Entram como pergunta em aberto.

## Reunião de apresentação (roteiro)

1. Escopo e o que não foi lido (exclusões, limites, heurística).
2. Inventário em uma página: módulos, linhas, dependências, versão Java sugerida.
3. Três a cinco achados revisados, com severidade e se a evidência é estrutural ou heurística.
4. O que foi dispensado e por quê.
5. Roadmap 30/60/90 e o que depende do cliente (acesso, ambiente, decisão).
6. Como os dados saem no fim do contrato.

Demonstração comercial de 5–10 minutos, com a fixture, está em `docs/DEMO_SCRIPT.md`.

## Preço

Hipótese de empacotamento, **não** preço de mercado e **não** proposta: um piloto fechado, fee fixo, um sistema, uma instalação, entrega do pacote acima. O número só existe depois de três conversas com compradores reais. Não há tabela neste repositório.
