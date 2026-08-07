# Testando com Postman

Não versionamos uma collection estática — cada serviço já expõe seu
OpenAPI ao vivo (extensão `quarkus-smallrye-openapi`), então o Postman
importa direto e nunca fica desatualizado.

**Collection já personalizada e não quer reimportar?** Veja
[`mudancas-manuais.txt`](mudancas-manuais.txt) — lista, por data, exatamente
o que mudar à mão na sua collection a cada mudança de contrato (endpoint
novo, campo removido, formato de erro diferente).

## 1. Importar o environment

`File > Import` → `docs/postman/financas-dev.postman_environment.json`.
Valores já vêm preenchidos com o client/usuário de teste que já existem em
`infra/keycloak/realm-financas.json` (dev only, não é segredo real).

## 2. Importar a collection de um serviço

Com o serviço rodando (`mvn quarkus:dev`), `File > Import` → aba **Link** →
`http://localhost:8081/q/openapi` (troque a porta pelo serviço). O Postman
gera a collection inteira a partir do contrato.

> Reimporte sempre que o contrato mudar (ex: `usuarioId` saiu do corpo de
> `POST /contas` e do query param de `GET /contas` em 2026-08-07 — agora
> vem do token). Uma collection importada antes disso vai mandar campos que
> a API já ignora/rejeita.

## 3. Pegar um token do Keycloak

Na collection importada (ou numa pasta), aba **Authorization** → tipo
**OAuth 2.0** → **Get New Access Token**:

| Campo | Valor |
|---|---|
| Grant Type | Password Credentials |
| Access Token URL | `{{keycloak_token_url}}` |
| Client ID | `{{keycloak_client_id}}` |
| Username | `{{keycloak_username}}` |
| Password | `{{keycloak_password}}` |

Requer Keycloak rodando (`docker compose up -d keycloak`). O token gerado
tem a role `usuario` — suficiente pra endpoints como
`POST /api/v1/contas`.

## 4. Testar endpoints internos (role `service`)

Endpoints como `POST /api/v1/contas/{id}/debitos` e `/creditos` são
serviço-a-serviço (ADR-0003) e exigem a role `service`, que o usuário de
teste acima não tem. Pra testar esses endpoints manualmente antes do
`transaction-service` existir de verdade, pegue um token via **Client
Credentials** (não Password) usando o client `transaction-service`, que já
tem `service account` habilitado e a role `service` atribuída em
`infra/keycloak/realm-financas.json`:

Na collection/request, aba **Authorization** → tipo **OAuth 2.0** → **Get
New Access Token**:

| Campo | Valor |
|---|---|
| Grant Type | Client Credentials |
| Access Token URL | `{{keycloak_token_url}}` |
| Client ID | `{{keycloak_service_client_id}}` |
| Client Secret | `{{keycloak_service_client_secret}}` |

O secret (`changeit`) é só valor de dev, já documentado como não-sensível em
`docs/architecture/security.md` — nunca reusar em produção.
