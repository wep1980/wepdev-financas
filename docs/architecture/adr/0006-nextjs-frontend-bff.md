# ADR-0006: Next.js como front-end web, assumindo o papel de BFF

Status: Aceita
Data: 2026-08-06

## Contexto

O front-end web seria React puro (SPA). O usuário definiu explicitamente
Next.js em vez disso. Além do ganho direto de SSR/SSG e roteamento de
fábrica, o `overview.md` já previa um "BFF/gateway web" como serviço
separado só para agregar chamadas aos microsserviços a favor do front-end —
com Next.js (App Router), Server Components e Route Handlers rodando no
próprio processo do front-end já cumprem esse papel de agregação, sem exigir
mais um serviço Java na arquitetura.

## Decisão

Front-end web em Next.js (React). Route Handlers/Server Actions do Next.js
fazem a agregação de chamadas aos microsserviços quando necessário (ex: tela
de dashboard que precisa de dados de `account-service` + `budget-service` +
`ai-service` numa única requisição do navegador). Não existe mais um serviço
"BFF/gateway web" separado no `overview.md`.

Mobile (React Native) **não** usa esse BFF — não há processo Next.js rodando
no dispositivo. O app mobile chama os microsserviços diretamente (mesmos
endpoints REST, mesma autenticação Keycloak/OIDC).

## Consequências

- Menos um serviço pra manter/deployar comparado ao plano original.
- O código de agregação do Next.js (Route Handlers) fica sujeito às mesmas
  regras de teste do restante do projeto (ver
  `docs/architecture/testing-strategy.md`) — não é "só front-end", é lógica
  de integração e precisa ser testada como tal (mock das chamadas HTTP aos
  microsserviços via MSW, testar o Route Handler isoladamente).
- Autenticação: o Next.js precisa lidar com o fluxo OIDC do Keycloak tanto
  para o usuário logado no navegador quanto para autenticar as chamadas
  server-side aos microsserviços — desenhar isso é tarefa da fatia 6 do
  roadmap, não decidido em detalhe aqui.
- Se web e mobile acabarem duplicando muita lógica de agregação (mobile
  batendo direto nos serviços, web batendo via Next.js), isso é aceitável no
  volume atual; revisitar com ADR novo se virar fonte real de duplicação/bug.
- UX, identidade visual, tipografia e paleta de cores: fora do escopo desta
  decisão, tratados separadamente quando a fatia de front-end começar (ver
  `docs/architecture/overview.md` seção 2.1).
