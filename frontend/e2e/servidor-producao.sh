#!/bin/sh
# Servidor do frontend para o E2E com a stack real: o mesmo build standalone da imagem (frontend/Dockerfile).
# Em `next dev`, cada rota compila na primeira visita (a de dimensões passa de 20 s com a máquina carregada) e o
# servidor pode ficar indisponível no meio da jornada: a ação de importar chegou a receber conexão recusada logo
# depois de uma recompilação. Com o build pronto antes do primeiro teste, o Playwright só começa quando o servidor
# já serve todas as rotas.
set -eu
cd "$(dirname "$0")/.."
npx --no-install next build
rm -rf .next/standalone/.next/static .next/standalone/public
cp -r .next/static .next/standalone/.next/static
cp -r public .next/standalone/public
# HOSTNAME do ambiente (nome da máquina, no CI) não serve para escutar: o servidor standalone usa esta variável.
HOSTNAME=0.0.0.0 exec node .next/standalone/server.js
