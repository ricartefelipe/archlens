FROM node:22.23.4-alpine3.24 AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --ignore-scripts

FROM node:22.23.4-alpine3.24 AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
ARG NEXT_PUBLIC_OIDC_ISSUER=http://localhost:8081/realms/archlens
ARG NEXT_PUBLIC_OIDC_CLIENT_ID=archlens-frontend
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL \
    NEXT_PUBLIC_OIDC_ISSUER=$NEXT_PUBLIC_OIDC_ISSUER \
    NEXT_PUBLIC_OIDC_CLIENT_ID=$NEXT_PUBLIC_OIDC_CLIENT_ID \
    AUTH_LOCAL_ENABLED=true
RUN npm run build

FROM node:22.23.4-alpine3.24 AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
# A imagem final só executa `node server.js`: o npm/corepack embutidos e seus pacotes não são usados
# e saem da imagem para não carregar vulnerabilidades de ferramenta de build.
RUN apk upgrade --no-cache \
    && rm -rf /usr/local/lib/node_modules /usr/local/bin/npm /usr/local/bin/npx /usr/local/bin/corepack \
    && addgroup -g 10001 -S archlens \
    && adduser -S -G archlens -u 10001 archlens
COPY --from=build --chown=archlens:archlens /app/public ./public
COPY --from=build --chown=archlens:archlens /app/.next/standalone ./
COPY --from=build --chown=archlens:archlens /app/.next/static ./.next/static
USER 10001:10001
EXPOSE 3000
HEALTHCHECK --interval=10s --timeout=5s --retries=12 \
  CMD wget -qO- http://127.0.0.1:3000/ || exit 1
CMD ["node", "server.js"]
