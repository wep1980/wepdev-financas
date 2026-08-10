# budget-service

Orçamento por categoria/mês e cálculo de "disponível pra gastar" (PRD
3.3). Contrato: [`docs/specs/budget-service.yaml`](../../docs/specs/budget-service.yaml).

**Status: scaffold só** — projeto Quarkus criado (`pom.xml`, Dockerfiles,
`application.properties` porta 8085), ainda sem domínio/persistência/REST
implementados. Ver `docs/tasks.md` (fatia 4) pro backlog detalhado.

## Regra de cálculo

Fórmula exata e o porquê de cada parcela documentados em
[ADR-0026](../../docs/architecture/adr/0026-regra-calculo-disponivel-para-gastar.md):

```
disponivelParaGastar(mês) =
    Σ saldo das contas CORRENTE/CARTEIRA (account-service)
  − Σ valorTotal das faturas FECHADA com vencimento no mês (card-service)
  − Σ valor das despesas recorrentes ATIVA com dataInicio <= fim do mês (transaction-service)
  − reserva (valor único, definido no próprio budget-service)
```

Orçamento por categoria (`/orcamentos`) é independente desse cálculo —
usa `transaction-service` `GET /transacoes/resumo-por-categoria` (endpoint
já existente) pra saber quanto já foi gasto numa categoria/mês.

## Integrações

Três clientes de saída, todos propagando o token do próprio usuário
autenticado (mesmo padrão de dois tokens do `document-service`/ADR-0025) —
nenhuma chamada precisa de role `service`, então, ao contrário do
`card-service`, não há `client_credentials` configurado nesse serviço:

- `account-service` — saldo das contas.
- `card-service` — faturas fechadas em aberto.
- `transaction-service` — despesas recorrentes ativas e resumo de gasto
  por categoria.

## Rodando no IntelliJ

Mesma configuração dos outros serviços: Maven Home apontado pro `mvnw` do
projeto, variáveis de ambiente de dev (`DB_USER`, `DB_PASSWORD`,
`KEYCLOAK_URL`) já têm default no `application.properties`, não precisa
configurar nada extra pra rodar `quarkus:dev`.
