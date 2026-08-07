# account-service

Contas financeiras (criar, listar, debitar/creditar saldo). Contrato:
[`docs/specs/account-service.yaml`](../../docs/specs/account-service.yaml).

## Rodando no IntelliJ

1. Abra a pasta raiz do repositório (`wepdev-financas`) no IntelliJ — se o
   `pom.xml` deste serviço não for detectado automaticamente, clique com o
   botão direito nele → **Add as Maven Project**.
2. Suba as dependências (na raiz do repo):
   ```bash
   docker compose up -d mysql kafka keycloak
   ```
   (o `account-service` já tem `Dockerfile` funcional no compose, mas pra
   dev diário é mais rápido rodar só a infra e o app em `quarkus:dev` local,
   como no passo 3 — `docker compose up -d account-service` builda a partir
   de `target/quarkus-app/`, então precisa rodar `mvn package` antes.)
3. Rode em modo dev — pelo terminal do IntelliJ, dentro desta pasta:
   ```bash
   mvn quarkus:dev
   ```
   Sobe em `http://localhost:8081`, com live reload a cada mudança salva.
   (Se instalar o plugin **Quarkus Tools** no IntelliJ, aparece um botão de
   run dedicado — não obrigatório, o comando Maven já resolve.)

## Testando no Postman

Ver [`docs/postman/README.md`](../../docs/postman/README.md) — resumo:
importe o environment `docs/postman/financas-dev.postman_environment.json`,
importe a collection direto de `http://localhost:8081/q/openapi`, pegue um
token OAuth2 (Password Credentials) do Keycloak usando as variáveis do
environment.

Swagger UI (teste manual sem Postman): `http://localhost:8081/q/swagger-ui`.

Os endpoints `POST /api/v1/contas/{id}/debitos` e `/creditos` são internos
(role `service`, ADR-0003) — pensados pro `transaction-service` chamar
serviço-a-serviço via client credentials, não pro front-end. Pra testar
manualmente, o client OIDC precisa ter a role `service` atribuída no realm
`financas` do Keycloak.

## Acessando o banco no DBeaver

Com `docker compose up -d mysql` rodando:

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Porta | `3307` (não 3306 — já tem um MySQL local na sua máquina; ver `docker-compose.yml`) |
| Database | `account_db` |
| Usuário | `financas` |
| Senha | `financas` |

Valores de dev (`.env.example` na raiz) — nunca os mesmos em produção
(`docs/architecture/security.md`).

## Rodando os testes

```bash
mvn test
```

Testes de integração (`@QuarkusTest`) sobem MySQL e Kafka automaticamente
via Quarkus Dev Services (Testcontainers) — não precisa do
`docker compose up` rodando pra isso, só pro `quarkus:dev` manual.

## Estrutura

```
src/main/java/br/com/wepdev/financas/account/
├── domain/           # regra de negócio, sem depender de framework
├── application/      # casos de uso, orquestram domain via ports
└── infrastructure/   # REST, persistência (Panache), Kafka — implementam os ports do domain
```
