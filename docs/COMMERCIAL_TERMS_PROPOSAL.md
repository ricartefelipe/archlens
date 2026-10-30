# Proposta de termos — não adotada

Documento para o proprietário decidir. **Não** é contrato, **não** concede licença e **não** define preço.

## Oferta que o software sustenta hoje

Diagnóstico arquitetural local de aplicações Java. O cliente (ou a consultoria no perímetro dele) roda o analisador offline, importa só o `analysis.json` e revisa achados, questionário, roadmap e relatório. Recomendações do motor são evidência para revisão humana.

Modalidades compatíveis com o código atual, à escolha do proprietário:

1. **Licença de uso instalada** no ambiente do cliente, sem cobrança dentro do produto.
2. **Serviço de diagnóstico** em que a ferramenta apoia o arquiteto e o entregável é o relatório revisado.

Não há assinatura, checkout nem marketplace.

## O que a entrega inclui

- CLI de análise estática, API, interface da jornada e Compose de demonstração.
- Perfis `local`, `demo` e `prod` documentados.
- Sample sintético `samples/legacy-java-demo`.
- Backup/restore documentados para o Postgres da instalação.

## O que a entrega não inclui

- Garantia de precisão, cobertura ou ausência de falso positivo.
- Execução do build, testes ou código do projeto analisado.
- Envio de código-fonte para fora da máquina de scan.
- IdP, certificado TLS, segredo de banco e domínio do cliente.
- Homologação jurídica (LGPD, DPA) ou certificação.
- Correção automática de código.
- Suporte com SLA e preço — ficam fora até o proprietário defini-los.

## Limites que o contrato deveria repetir

- Maven resolve só propriedades locais; parent e BOM remotos aparecem como limitação.
- Gradle não avalia Groovy/Kotlin nem version catalogs.
- Achados, scores e narrativa são apoio à decisão.
- Produção não aceita identidade por header.
- Narrativa de provedor externo é opt-in e envia só contagens agregadas.
- Retenção legal (`ARCHLENS_LEGAL_HOLD`) e retenção de auditoria são configuração, não política jurídica pronta.

## Rascunho de cláusula que o proprietário pode reescrever

> O software é fornecido para diagnóstico arquitetural no ambiente do licenciado. O licenciado revisa os achados antes de qualquer decisão de modernização. O fornecedor não afirma que um sinal heurístico é uma falha comprovada. Código-fonte do sistema analisado não é parte do artefato importado.

Cabe ao proprietário definir território, prazo, número de instalações, confidencialidade, responsabilidade e a licença dos fontes (proprietária, ou uma licença open source compatível com o inventário de dependências).
