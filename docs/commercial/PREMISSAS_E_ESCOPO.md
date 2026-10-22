# Premissas e escopo — modelo para o piloto

Texto de trabalho para o contrato com o cliente. Não é minuta jurídica pronta e não substitui revisão do jurídico das duas partes. Ajuste nomes, prazos e sistema antes de assinar.

## Partes

- Fornecedor: quem opera o diagnóstico assistido.
- Cliente: dono do sistema Java e do ambiente onde a análise ocorre.

## Confidencialidade

- O código-fonte permanece no ambiente do cliente. O fornecedor não o copia para repositório, laptop pessoal ou nuvem própria, salvo autorização escrita e caminho explícito.
- O artefato padrão que pode sair da máquina de scan é o `analysis.json` (metadados, hashes e sinais), e só para a instalação ArchLens combinada.
- Amostra de demonstração é exclusivamente `samples/legacy-java-demo` ou outro material que o cliente declare compartilhável.
- Achados, relatórios e notas de reunião seguem o NDA do cliente. Na falta de NDA próprio, este anexo vale como compromisso mínimo até o jurídico formalizar.

## Ambiente

- CLI no host indicado pelo cliente, com permissão de leitura e sem permissão de deploy.
- Backend, Postgres e frontend na rede do cliente, com OIDC corporativo, TLS e secrets fora do Git.
- Uma organização por instalação. Papéis: ADMIN (retirada de dados e configuração), ARCHITECT (revisão e relatório), VIEWER (leitura).
- O Compose de demonstração do repositório **não** é o ambiente do piloto.

## O que está dentro

- Um sistema (um repositório ou um conjunto de módulos declarado no kickoff).
- Scan estático, importação, questionário, revisão de achados, roadmap e um relatório publicado.
- Uma reunião de apresentação.
- Retirada dos dados da avaliação ao fim, se o cliente pedir, pelo procedimento em `OPERATIONS.md`.

## O que está fora

- Executar, compilar ou testar o sistema alvo. O analyzer não roda o código.
- Precisão absoluta, ausência de falso positivo/negativo ou cobertura de tudo que um AST completo veria.
- Gradle com catalog resolvido, parents Maven baixados da internet, bytecode.
- Corrigir o legado, implementar a modernização, operar produção ou assumir incidentes do sistema do cliente.
- Pentest, análise dinâmica, dados pessoais em bancos, segredos de cofre e binários.
- SaaS multi-cliente, alta disponibilidade, certificação e parecer jurídico/regulatório.
- Envio de código ou de `analysis.json` a modelo de linguagem. LLM permanece desligado salvo aditivo.

## Limites técnicos que o cliente aceita ao autorizar

- Arquivos acima de 2 MiB, binários e diretórios de build são ignorados. O JSON registra o diagnóstico de limite quando ocorrer.
- Symlink para fora da raiz é ignorado. Ciclo de symlink não é seguido.
- `--exclude` lista o que o cliente não quer ler. O que não foi excluído e estiver sob a raiz pode ser lido como texto.
- Importação com hash divergente é recusada. Não se "ajusta" hash.
- Scores apoiam a conversa. Não são nota de qualidade do sistema.

## Responsabilidades

| Fornecedor | Cliente |
|------------|---------|
| Operar CLI e plataforma conforme este escopo | Indicar o host, o escopo e as exclusões |
| Explicar heurística versus evidência estrutural | Disponibilizar um arquiteto que revise achados |
| Não exfiltrar fonte | Prover IdP, TLS, backup e janela de acesso |
| Retirar os dados quando solicitado | Decidir o que pode ser apresentado ao patrocinador |
| Dizer o que não foi possível medir | Não tratar o relatório como auditoria certificada |

## Suporte durante o piloto

Hipótese a negociar, não SLA vigente:

- Canal único e horário comercial do fuso do cliente.
- Resposta a bloqueio de ambiente em um dia útil; defeito que impeça a entrega do relatório em dois dias úteis.
- Fora do horário não há plantão.
- Depois da retirada dos dados, o suporte do piloto encerra, salvo aditivo.

## Encerramento

1. Relatório publicado entregue ao cliente.
2. ADMIN executa a retirada ou o cliente assume a guarda do Postgres, por escrito.
3. Acessos do fornecedor no IdP e no host são revogados pelo cliente.
4. Cópias de trabalho do `analysis.json` no host de scan são apagadas se o contrato não mandar retê-las.
