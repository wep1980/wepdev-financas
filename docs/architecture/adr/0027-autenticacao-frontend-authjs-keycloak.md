# ADR-0027: Autenticação do front-end web via Auth.js (NextAuth v5) + provider Keycloak

Status: Aceita
Data: 2026-08-10

## Contexto

ADR-0006 decidiu Next.js como front-end web, com Route Handlers/Server
Actions assumindo o papel de BFF, mas deixou explicitamente em aberto
"como o Next.js lida com o fluxo OIDC do Keycloak tanto para o usuário
logado no navegador quanto para autenticar as chamadas server-side aos
microsserviços" — decisão adiada pra quando a fatia 6 (front-end)
começasse. Chegou a hora.

O sistema já usa Keycloak (OIDC) como identity provider desde o início
(ADR-0003) — todos os seis serviços de back-end validam Bearer token JWT
emitido por ele. O front-end web precisa: (1) redirecionar o usuário pro
login do Keycloak, (2) manter uma sessão no navegador depois do login, (3)
obter e renovar o access token pra propagar nas chamadas que os Route
Handlers fazem aos microsserviços em nome do usuário logado (mesmo padrão
de "propagar o token do próprio usuário" já usado em toda comunicação
síncrona entre serviços, ver `overview.md` seção 8).

Duas opções reais: implementar o fluxo (Authorization Code + PKCE, troca de
código por token, refresh, cookie de sessão assinado) na mão via Route
Handlers customizados, ou usar uma biblioteca madura do ecossistema
Next.js. Reimplementar OIDC na mão é superfície de risco real pra bug de
segurança (sessão mal invalidada, token vazando pra client-side, refresh
mal feito) — o tipo de coisa que a seção "Dados financeiros são sensíveis"
do `CLAUDE.md` pede pra tratar como requisito não-negociável, não
"depois a gente vê".

## Decisão

**Auth.js (NextAuth v5)** com o provider OIDC genérico apontando pro realm
`financas` do Keycloak (mesma abordagem de qualquer client OIDC padrão —
não precisa de um provider dedicado do Keycloak, o genérico cobre
Authorization Code + PKCE, refresh token e discovery de metadata via
`/.well-known/openid-configuration`).

- **Sessão**: JWT assinado pelo próprio Next.js (estratégia `jwt` do
  Auth.js, não sessão em banco) guardado em cookie `httpOnly`,
  `secure` (produção) — nunca acessível via JavaScript do navegador.
- **Access token do Keycloak**: guardado dentro da sessão do Auth.js
  (server-side only, via callback `jwt`/`session`), nunca exposto ao
  client-side. Route Handlers/Server Actions leem a sessão server-side
  (`auth()`) pra pegar o access token e propagar como `Authorization:
  Bearer` nas chamadas aos microsserviços — mesmo princípio dos outros
  serviços (ex: `budget-service`→`account-service`), só que a origem do
  token aqui é a sessão do navegador, não um header de requisição HTTP
  recebido.
- **Refresh**: callback `jwt` do Auth.js verifica expiração e troca pelo
  `refresh_token` automaticamente antes do access token expirar — sem
  forçar o usuário a logar de novo a cada poucos minutos (o access token
  do Keycloak tem vida curta, ver `security.md`).
- **Client Keycloak**: reaproveita o client `web-app` já existente no
  realm (`publicClient: true`, `standardFlowEnabled: true`,
  `redirectUris: ["http://localhost:3000/*"]`) — nenhum client novo
  necessário, é exatamente o client que já foi criado pra esse propósito
  desde o início do projeto.
- **Logout**: Auth.js limpa a sessão local E redireciona pro endpoint de
  logout do Keycloak (`end_session_endpoint`), encerrando a sessão SSO
  também — evita "logout" que só limpa cookie local mas deixa o usuário
  ainda autenticado no Keycloak.

## Consequências

- Menos código pra manter e menos superfície de bug de segurança do que
  reimplementar OIDC na mão — trade-off aceito: mais uma dependência de
  runtime (`next-auth`), mas é a biblioteca de fato padrão do ecossistema
  Next.js pra esse problema, mantida ativamente.
- Route Handlers que chamam os microsserviços precisam ler a sessão
  (`auth()`) antes de montar a chamada — se a sessão não existir ou o
  token não puder ser renovado, o Route Handler responde 401 e o
  middleware do Next.js redireciona pro login; nenhuma chamada aos
  microsserviços deve acontecer sem token válido.
- App mobile (React Native, fatia 7) **não** usa Auth.js — continua
  batendo direto nos microsserviços com seu próprio fluxo OIDC (ADR-0006
  já estabelecia isso), então essa decisão não se propaga pra lá.
- Testes de Route Handler que dependem de sessão mockam `auth()` (ver
  `docs/architecture/testing-strategy.md` seção 3 — MSW pra chamada HTTP
  aos microsserviços, mock direto de `auth()` pra sessão), não sobem um
  Keycloak real — mesmo princípio de "adapter fino não testado contra
  infra real em teste unitário/componente" já usado no back-end.
