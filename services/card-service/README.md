# card-service

Gerencia cartões de crédito, faturas e parcelamento. Independente de
`TipoConta.CARTAO_CREDITO` do `account-service` — ver
[ADR-0022](../../docs/architecture/adr/0022-card-service-independente-de-conta.md).
Contrato: [`docs/specs/card-service.yaml`](../../docs/specs/card-service.yaml).

## Relação com o account-service

Todo `Cartao` tem um `contaPagamentoId` apontando pra uma `Conta`
(`CORRENTE`/`POUPANCA`/`CARTEIRA`) do `account-service` — nunca uma FK real
(*database-per-service*, ADR-0001), só uma referência lógica confirmada de
forma síncrona. Ao criar ou atualizar um cartão, o `card-service` chama
`GET /api/v1/contas/{id}` com o **token do próprio usuário repassado**
(`PropagarAutorizacaoHeadersFactory`) — reusa o 404 do `account-service`
como gate de autorização, mesmo padrão já usado pelo `transaction-service`.

Ao pagar uma fatura (`POST /faturas/{id}/pagar`), o débito na
`contaPagamentoId` também é síncrono — primeiro reconfirma posse com o
token do usuário, depois debita com token de serviço
(`client_credentials`, role `service`, `AccountServiceInternoClient`),
mesmo padrão de dois tokens do `transaction-service`.

## Fatura e parcelamento

Não existe endpoint de criação manual de fatura — `LancarCompraUseCase`
cria a `Fatura` automaticamente (uma por `cartaoId` + competência
`AAAA-MM`) na primeira compra lançada naquela competência. Uma compra
parcelada gera uma `Parcela` por competência consecutiva (não existe
classe "Compra" persistida — é só o agrupamento lógico das `Parcela`s que
compartilham o mesmo `compraId`). Arredondamento de
`valorTotal / quantidadeParcelas` (2 casas, `HALF_UP`) absorve a
diferença na última parcela.

`FecharFaturasVencidasJob` (`quarkus-scheduler`, cron diário) fecha toda
fatura `ABERTA` cuja `dataFechamento` já passou — núcleo testável
(`FecharFaturasVencidasUseCase`) recebe a data como parâmetro, mesmo
padrão do `GerarOcorrenciasRecorrentesJob` do `transaction-service`.

## Rodando no IntelliJ

1. Abra a pasta raiz do repositório (`wepdev-financas`) no IntelliJ.
2. Suba as dependências (na raiz do repo):
   ```bash
   docker compose up -d mysql keycloak
   ```
3. **O `account-service` também precisa estar rodando** — este serviço
   depende dele pra confirmar posse de `contaPagamentoId`:
   ```bash
   cd services/account-service && mvn quarkus:dev
   ```
4. Em outro terminal, rode este serviço:
   ```bash
   mvn quarkus:dev
   ```
   Sobe em `http://localhost:8083`, com live reload a cada mudança salva.

## Testando no Postman

Ver [`docs/postman/README.md`](../../docs/postman/README.md) — mesmo fluxo
de token do `account-service` (Password Credentials, role `usuario`).
Swagger UI: `http://localhost:8083/q/swagger-ui`.

## Acessando o banco no DBeaver

Com `docker compose up -d mysql` rodando:

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Porta | `3307` |
| Database | `card_db` |
| Usuário | `financas` |
| Senha | `financas` |

## Rodando os testes

```bash
mvn test
```

Testes de integração (`@QuarkusTest`) sobem MySQL automaticamente via
Quarkus Dev Services — não precisa do `account-service` real rodando: o
port `AccountServiceClient` é substituído por um mock via `QuarkusMock`
(ver `CartaoResourceTest`), cobrindo sucesso e 404 sem depender de rede.

## Estrutura

```
src/main/java/br/com/wepdev/financas/card/
├── domain/           # regra de negócio, sem depender de framework
├── application/      # casos de uso, orquestram domain via ports
└── infrastructure/   # REST, persistência (Panache), client HTTP pro account-service
```
