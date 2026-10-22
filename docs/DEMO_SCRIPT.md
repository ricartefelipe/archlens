# Roteiro de demonstração ArchLens (≤ 10 minutos)

**Objetivo:** mostrar diagnóstico arquitetural rastreável a partir de análise **offline**, sem enviar código-fonte do cliente.  
**Use apenas** a fixture `samples/legacy-java-demo` — nunca código real de cliente nesta demo.

## Antes (≈ 2 min, fora do pitch)

```bash
# Linux/macOS — stack demo
export DOCKER_HOST=unix:///var/run/docker.sock   # se Docker Desktop inativo
docker compose -f infra/docker-compose.yml up --build -d
curl -fsS http://localhost:8080/q/health/ready

# Analysis offline (pode ser feito com antecedência)
cd analyzer && ./mvnw -pl cli -am package -DskipTests
java -jar cli/target/archlens-analyzer-cli-*.jar scan \
  --path ../samples/legacy-java-demo --out /tmp/analysis.json
java -jar cli/target/archlens-analyzer-cli-*.jar validate --file /tmp/analysis.json
```

Windows (PowerShell): `.\scripts\stack-up.ps1` e os mesmos comandos Java no analyzer.

Abra http://localhost:3000.

---

## Minuto 0–1 — Problema

> “Temos um Java legado pouco documentado. Precisamos de inventário, riscos e um plano de modernização **sem** subir o código para a nuvem.”

Mostre o sample (estrutura Maven pequena) e diga que o analyzer roda **só leitura, offline**.

## Minuto 1–2 — Análise offline

Mostre `/tmp/analysis.json` (metadados, `contentHash`, secrets só com hash).  
Reforce: o valor fictício do sample **não** aparece no JSON.  
Se alguém alterar o JSON e mantiver o hash antigo, a importação recusa (`contentHash` divergente).

## Minuto 2–3 — Login e import

1. Login de demonstração: `architect` / `architect123` (só existe porque o Compose demo declara `ARCHLENS_ALLOW_DEMO_CREDENTIALS=true`; instalação de cliente não usa isso)
2. Criar avaliação (nome livre; pode ter acento, por exemplo Modernização)
3. Importar `/tmp/analysis.json`
4. Confirmar “Importação concluída”

## Minuto 3–5 — Inventário e findings

1. Inventário: módulos e dependências
2. Achados: severidade, evidência e se o achado é heurístico ou estrutural
3. Revisar um finding → status **APPROVED** (isso é a revisão do arquiteto, não a detecção)

## Minuto 5–7 — Questionário, scores e roadmap

1. Questionário: preencher notas e salvar  
2. Dimensões: scores + rationale (assistido, não “verdade absoluta”)  
3. Roadmap: adicionar 1 item de modernização e salvar  

## Minuto 7–9 — Relatório e publicação

1. Gerar relatório  
2. Abrir HTML: seções **Detecção automatizada** e **Revisão do arquiteto**; frase com acento ("ação")
3. Baixar PDF e mostrar o mesmo texto acentuado
4. Gerar de novo (segunda versão) e **publicar** um snapshot
5. Mostrar que republicar fica bloqueado
6. Auditoria: import, revisão, publicação
7. Se estiver como `admin`: "Retirar dados da avaliação" apaga o diagnóstico e redige a auditoria. Faça isso só num ensaio, não na avaliação que você acabou de apresentar.  

## Minuto 9–10 — Segurança e limites honestos

1. Logout → login `viewer` / `viewer123`  
2. Mostrar que criar/escrever não está disponível (e API 403)  
3. Frases obrigatórias:
   - Demo Compose **não é produção** (auth por headers no backend demo).
   - Findings usam **heurísticas**; arquiteto valida.
   - Código do cliente permanece no ambiente do cliente.
   - Pronto para **piloto assistido**, não SaaS público.

---

## Encerramento (15 s)

> “Entregável: diagnóstico rastreável + relatório + roadmap — com revisão humana e análise offline.”

Pare a stack **sem** apagar dados: `docker compose -f infra/docker-compose.yml down` ou `.\scripts\stack-down.ps1`.
