# Banco do ambiente local. A imagem oficial traz o gosu compilado com um Go antigo (26 achados altos e críticos, 1
# crítico) só para o entrypoint trocar de root para postgres; aqui o container já inicia como postgres, então o gosu sai.
# postgres:16-alpine, fixada pelo digest. Correções de segurança da base publicadas depois da imagem entram em cada build.
FROM postgres@sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab
RUN apk upgrade --no-cache && rm -f /usr/local/bin/gosu
USER postgres
