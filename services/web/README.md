# web

Front-end web em Next.js (App Router) — assume o papel de BFF, agregando
os seis microsserviços via Route Handlers/Server Components (ADR-0006).
Ver `docs/tasks.md` (fatia 6) pro backlog detalhado.

**Status: scaffold + autenticação + shell + conta + transação +
dashboard + documento + refinamento visual + chat com IA** — projeto
criado (Next.js 16 + TypeScript + Tailwind CSS v4 + shadcn/ui), design
system vivo em
[`docs/architecture/design-system.md`](../../docs/architecture/design-system.md)
(tokens, paleta, tipografia Inter, padrões de página). Menu lateral
fixo (desktop)/off-canvas (mobile) com usuário/logout no rodapé
(`app/(app)/app-sidebar.tsx`). Login/logout via Keycloak (Auth.js)
funcionando de ponta a ponta, toda rota protegida por padrão. `/contas`
(item 4), `/transacoes` (item 5), `/` (item 6 — dashboard),
`/documentos` (item 7 — upload de fatura PDF com polling de status) e
`/chat` (item 9 — conversa com o agente de IA, validado com LLM real
via Ollama) cobrem os seis microsserviços. Só falta o item 10
(fechamento da fatia — CI, Docker Compose, diagramas) pra fatia 6
terminar.

## Stack

- **Next.js 16** (App Router, Turbopack) + React 19 + TypeScript.
- **Tailwind CSS v4** + **shadcn/ui** (componentes copiados pro repo em
  `components/ui/`, não dependência de runtime — decisão do usuário,
  2026-08-10).
- **Auth.js (NextAuth v5 beta)** com o provider nativo do Keycloak
  (ADR-0027) — `auth.ts` (config), `proxy.ts` (protege toda rota exceto
  `/login`), `lib/auth-token.ts` (access token pro Route Handler, nunca
  exposto via `/api/auth/session`), `lib/auth-token-refresh.ts` (renovação
  automática, testada), `lib/auth-actions.ts` (`entrar`/`sair`, logout
  também encerra a sessão SSO no Keycloak).
- **AwesomeAPI** (`economia.awesomeapi.com.br`, pública, sem chave) —
  cotação do dólar no dashboard (`lib/cambio.ts`). Única chamada HTTP
  do projeto que não é a um dos seis microsserviços: sem token, com
  cache de 5min (dado público, igual pra todo usuário — diferente dos
  clients de microsserviço, que nunca cacheiam por serem dado por
  usuário).
- **Vitest + React Testing Library + MSW** para teste de
  componente/hook/integração com API mockada na borda HTTP. Server
  Components `async` (ex: `app/page.tsx`, que chama `auth()`) não são
  testáveis via Vitest — [confirmado na doc oficial do
  Next.js](node_modules/next/dist/docs/01-app/02-guides/testing/vitest.md),
  recomendação é E2E pra esses casos; o que sobra testável vira função
  pura extraída (ex: `lib/auth-token-refresh.ts`).

> Next.js 16 tem mudanças de convenção relevantes em relação a versões
> anteriores — ex: `proxy.ts` substituiu `middleware.ts`. Ver
> `node_modules/next/dist/docs/` (guias embutidos no próprio pacote)
> antes de mexer em arquivo de convenção de roteamento.

## Rodando local

Precisa da infra do `docker-compose.yml` raiz no ar (Keycloak em
particular — `docker compose up -d keycloak`).

```bash
cp .env.example .env.local   # já funciona sem editar nada, é dev
npm install
npm run dev      # http://localhost:3000 — redireciona pra /login sozinho
npm run test     # Vitest
npm run lint     # ESLint
npm run build    # build de produção (gera .next/standalone)
```

Login de teste: `usuario.teste` / `financas123` (mesmo usuário usado em
todo o resto do projeto via Postman, `docs/postman/README.md`).

## Docker

```bash
docker build -t web:ci .
docker run -p 3000:3000 web:ci
```

Multi-stage: builder roda `npm ci` + `npm run build` (output
`standalone`, configurado em `next.config.ts`), a imagem final só copia
o server autocontido + assets estáticos — sem `node_modules` completo
no runtime, mesmo espírito do `Dockerfile.jvm` dos serviços Java.
