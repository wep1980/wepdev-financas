# Testando com Postman — passo a passo completo

Não versionamos uma collection estática — cada serviço já expõe seu
OpenAPI ao vivo (extensão `quarkus-smallrye-openapi`), então o Postman
importa direto e nunca fica desatualizado.

**Collection já personalizada e não quer reimportar?** Veja
[`mudancas-manuais.txt`](mudancas-manuais.txt) — lista, por data, exatamente
o que mudar à mão na sua collection a cada mudança de contrato (endpoint
novo, campo removido, formato de erro diferente). Se está importando pela
primeira vez, ignore esse arquivo e siga o passo a passo abaixo.

## 0. Pré-requisito: infra e serviços no ar

```bash
# infra (na raiz do repo)
docker compose up -d mysql kafka zookeeper keycloak

# account-service (num terminal)
cd services/account-service && mvn quarkus:dev

# transaction-service (em outro terminal — depende do account-service pra registrar transação)
cd services/transaction-service && mvn quarkus:dev

# card-service (em outro terminal — depende do account-service pra confirmar contaPagamentoId)
cd services/card-service && mvn quarkus:dev
```

Confirme que todos estão respondendo antes de seguir:

```bash
curl http://localhost:8080/realms/financas/.well-known/openid-configuration   # Keycloak
curl http://localhost:8081/q/health                                          # account-service
curl http://localhost:8082/q/health                                          # transaction-service
curl http://localhost:8083/q/health                                          # card-service
```

Alternativa: `docker compose up -d --build account-service transaction-service
card-service` sobe tudo containerizado, sem precisar de terminal separado
por serviço — mais lento pra iterar (precisa rebuild a cada mudança de
código), mas único comando.

## 1. Importar o environment

`File > Import` → `docs/postman/financas-dev.postman_environment.json`.
Valores já vêm preenchidos com o client/usuário de teste que já existem em
`infra/keycloak/realm-financas.json` (dev only, não é segredo real).
Confirme que o environment **financas-dev** está selecionado (canto
superior direito do Postman) antes de continuar.

## 2. Importar as collections (uma por serviço)

Com cada serviço rodando, `File > Import` → aba **Link**:

- account-service: `http://localhost:8081/q/openapi`
- transaction-service: `http://localhost:8082/q/openapi`

O Postman gera a collection inteira a partir do contrato OpenAPI.

## 3. Pegar um token de usuário (role `usuario`)

Serve pra praticamente tudo — criar/listar/buscar/atualizar/excluir conta,
registrar/listar/cancelar transação. Numa request ou pasta da collection,
aba **Authorization** → tipo **OAuth 2.0** → **Get New Access Token**:

| Campo | Valor |
|---|---|
| Grant Type | Password Credentials |
| Access Token URL | `{{keycloak_token_url}}` |
| Client ID | `{{keycloak_client_id}}` |
| Username | `{{keycloak_username}}` |
| Password | `{{keycloak_password}}` |

O token expira em 5 minutos (`expires_in: 300`) — se começar a tomar 401
do nada no meio de um teste, é isso: gere um novo.

## 4. Pegar um token de serviço (role `service`) — só pros endpoints internos

Endpoints como `POST /api/v1/contas/{id}/debitos` e `/creditos` são
serviço-a-serviço (ADR-0003) e o token do passo 3 **não** tem a role pra
eles — na prática, você não deveria precisar chamar esses diretamente (é o
`transaction-service` quem chama), mas pra testar isolado:

| Campo | Valor |
|---|---|
| Grant Type | Client Credentials |
| Access Token URL | `{{keycloak_token_url}}` |
| Client ID | `{{keycloak_service_client_id}}` |
| Client Secret | `{{keycloak_service_client_secret}}` |

O secret (`changeit`) é só valor de dev, já documentado como não-sensível
em `docs/architecture/security.md` — nunca reusar em produção.

## 5. Roteiro sugerido pra testar o sistema de ponta a ponta

Com o token de usuário (passo 3) aplicado, nessa ordem:

1. `POST /api/v1/contas` — cria uma conta (não envie `usuarioId`, vem do
   token). Guarde o `id` da resposta.
2. `GET /api/v1/contas` — confirma que a conta criada aparece na lista.
3. `GET /api/v1/contas/{id}` — busca ela especificamente.
4. `PUT /api/v1/contas/{id}` — atualiza nome/instituição.
5. Na collection do **transaction-service**: `POST /api/v1/transacoes`
   usando o `contaId` do passo 1, `tipo: DESPESA`. Guarde o `id` da
   transação.
6. `GET /api/v1/contas/{id}` (account-service de novo) — confirme que o
   saldo baixou pelo valor da transação.
7. `GET /api/v1/transacoes` — confirme que a transação aparece.
8. `DELETE /api/v1/transacoes/{id}` — cancela. Repita o passo 6: o saldo
   deve voltar ao valor original.
9. `DELETE /api/v1/contas/{id}` (account-service) — exclui (lógico) a
   conta usada no teste.

Isso exercita toda a superfície hoje implementada dos dois serviços numa
sequência só.

## 6. Erros comuns

| Sintoma | Causa provável |
|---|---|
| 401 em tudo, do nada | Token expirou (5 min) — gere outro (passo 3/4) |
| 401 só quando testando via `docker compose up` (containers, não `quarkus:dev`) | Configuração de issuer do Keycloak — já corrigido no `docker-compose.yml`/`application.properties` (`KC_HOSTNAME` + `discovery-enabled=false`, ver `docs/historico.md` 2026-08-07); se voltar a acontecer, comece verificando isso |
| 404 numa conta/transação que você acabou de criar | Conferindo com o token de outro usuário/client — `usuarioId` vem do `sub` do token, cada client de teste é "um usuário" diferente |
| 403 num endpoint `/debitos` ou `/creditos` | Token sem a role `service` — use o token do passo 4, não o do passo 3 |
| `Invalid URI`/erro estranho na URL | Variável `baseUrl` da collection (gerada automaticamente pelo import do OpenAPI) desalinhada — edite a collection → Variables → confirme que aponta pra `http://localhost:8081` (ou 8082) |
| Postman pede accented characters e a request falha com 400 vazio | Problema de encoding ao montar o JSON manualmente fora do Postman (ex. copiar/colar em terminal); dentro do Postman normalmente não acontece |
