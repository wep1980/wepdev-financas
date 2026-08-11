# Multi-stage — builder instala tudo (incluindo devDependencies) e roda
# "next build", a imagem final só copia o output "standalone" (server.js
# autocontido, ver next.config.ts) + os assets estáticos. Mesmo espírito
# do Dockerfile.jvm dos serviços Java: imagem final enxuta, sem
# ferramenta de build.

FROM node:24-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM node:24-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
RUN addgroup --system --gid 1001 nodejs \
    && adduser --system --uid 1001 nextjs

COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static
COPY --from=builder --chown=nextjs:nodejs /app/public ./public

USER nextjs
EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

CMD ["node", "server.js"]
