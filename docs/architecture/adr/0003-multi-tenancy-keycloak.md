# ADR-0003: Multi-tenancy via Keycloak/OIDC desde o início

Status: Aceita
Data: 2026-08-06

## Contexto

Definido explicitamente pelo usuário: o sistema deve nascer multi-usuário
(outras pessoas podem se cadastrar), não só uso pessoal. Isso muda requisitos
de isolamento de dados e autenticação desde a primeira fatia vertical — não
dá pra "adicionar multi-tenancy depois" sem reescrever a camada de acesso a
dado de todo serviço.

## Decisão

Manter Keycloak (OIDC) como já estava no README original. Todo dado
particionado por `usuarioId`, extraído do token (subject), nunca de parâmetro
de request não validado contra o token. Roles: `usuario` (endpoints
públicos), `admin` (gestão), `service` (chamada interna serviço-a-serviço via
client credentials, nunca exposto ao front-end). Detalhe em
`docs/architecture/overview.md` seção 5.

## Consequências

- Todo endpoint novo precisa, desde o dia 1, filtrar por `usuarioId` do
  token — isso vira item obrigatório de code review/definição de pronto.
- Setup de dev local exige Keycloak rodando (já está no `docker-compose.yml`,
  realm `financas` pré-configurado) — mais fricção que auth simplificada,
  aceito conscientemente.
- Habilita, sem retrabalho, o cenário de outras pessoas usarem o sistema mais
  adiante.
