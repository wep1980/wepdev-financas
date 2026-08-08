# transaction-service

Registra transações (receitas e despesas) e reflete o efeito no saldo da
conta correspondente, chamando o `account-service` de forma síncrona.
Contrato: [`docs/specs/transaction-service.yaml`](../../docs/specs/transaction-service.yaml).

## Como funciona a chamada síncrona pro account-service

`RegistrarTransacaoUseCase` chama o `account-service` **antes** de
persistir a transação — se a chamada falhar (conta não encontrada, saldo
insuficiente, indisponibilidade), nada é salvo (sem transação "fantasma").
Duas chamadas diferentes, dois tokens diferentes:

1. `GET /api/v1/contas/{id}` — confirma que a conta existe e pertence ao
   usuário autenticado. Usa o **token do próprio usuário**, repassado do
   request que chegou (`PropagarAutorizacaoHeadersFactory`) — reusa o 404
   do `account-service` como gate de autorização, não reimplementa a
   checagem aqui.
2. `POST /api/v1/contas/{id}/debitos` ou `/creditos` — aplica o ajuste de
   saldo de verdade. Usa um **token de serviço** (`client_credentials`,
   client `transaction-service` no Keycloak, role `service`), obtido
   automaticamente pelo `quarkus-rest-client-oidc-filter`.

## Rodando no IntelliJ

1. Abra a pasta raiz do repositório (`wepdev-financas`) no IntelliJ.
2. Suba as dependências (na raiz do repo):
   ```bash
   docker compose up -d mysql kafka keycloak
   ```
3. **O `account-service` também precisa estar rodando** — este serviço
   depende dele pra registrar qualquer transação:
   ```bash
   cd services/account-service && mvn quarkus:dev
   ```
4. Em outro terminal, rode este serviço:
   ```bash
   mvn quarkus:dev
   ```
   Sobe em `http://localhost:8082`, com live reload a cada mudança salva.

## Testando no Postman

Ver [`docs/postman/README.md`](../../docs/postman/README.md) — mesmo fluxo
de token do `account-service` (Password Credentials, role `usuario`).
Swagger UI: `http://localhost:8082/q/swagger-ui`.

## Acessando o banco no DBeaver

Com `docker compose up -d mysql` rodando:

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Porta | `3307` |
| Database | `transaction_db` |
| Usuário | `financas` |
| Senha | `financas` |

## Rodando os testes

```bash
mvn test
```

Testes de integração (`@QuarkusTest`) sobem MySQL e Kafka automaticamente
via Quarkus Dev Services — não precisa do `account-service` real rodando:
o port `AccountServiceClient` é substituído por um mock via `QuarkusMock`
(ver `TransacaoResourceTest`), cobrindo sucesso, 404 e 422 sem depender de
rede.

## Estrutura

```
src/main/java/br/com/wepdev/financas/transaction/
├── domain/           # regra de negócio, sem depender de framework
├── application/      # casos de uso, orquestram domain via ports
└── infrastructure/   # REST, persistência (Panache), Kafka, client HTTP pro account-service, scheduling
```
