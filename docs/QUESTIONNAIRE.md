# Questionário de modernização

Versão inicial: `1.0.0` (YAML no backend).

## Dimensões influenciadas

| Dimensão | Perguntas típicas |
|----------|-------------------|
| Business criticality | Impacto de downtime, regulatório |
| Team capability | Experiência com cloud/containers/Spring Boot/Quarkus |
| Risk tolerance | Janela de mudança, apetite a big-bang |
| Operability | Observabilidade atual, deploy frequency |
| Coupling | Integrações síncronas críticas |

## Como influencia scores

Cada resposta mapeia para um peso numérico. O score da dimensão combina:

`score = clamp(0..100, base_from_findings * (1 - w) + questionnaire_factor * w)`

Premissa MVP: achados e inventário definem base; questionário ajusta 20–40% do peso conforme dimensão.

## Versionamento

Mudanças incompatíveis incrementam versão major; avaliações guardam `questionnaireVersion` usada nas respostas.
