# ADR-0022: `card-service` é independente de `TipoConta.CARTAO_CREDITO`

Status: Aceita
Data: 2026-08-08

## Contexto

`account-service` já tem `TipoConta.CARTAO_CREDITO` desde a fatia 1 —
cadastro simples de conta, sem conceito de fatura, fechamento, vencimento
ou parcelamento. A fatia 2 introduz `card-service`, responsável por
"cartões de crédito, faturas, parcelamento" (`docs/architecture/overview.md`),
com banco próprio (`card_db`, ADR-0001 — database-per-service). Precisava
ficar claro como esses dois conceitos de "cartão" se relacionam antes de
desenhar o contrato do `card-service` (`docs/specs/card-service.yaml`).

## Decisão

`card-service` é **totalmente independente** de `TipoConta.CARTAO_CREDITO`.
O aggregate `Cartao` do `card-service` não referencia nem existe como
`Conta` do `account-service`. Em vez disso, todo `Cartao` tem um
`contaPagamentoId` apontando pra uma `Conta` do `account-service` — sempre
`CORRENTE`, `POUPANCA` ou `CARTEIRA` (nunca `CARTAO_CREDITO`, evita
circularidade) — que é debitada de forma **síncrona** quando o usuário
paga a fatura, mesmo padrão de chamada síncrona já usado por
`transaction-service` → `account-service` (débito/crédito antes de
persistir, sem "fatura fantasma" se a chamada falhar).

`TipoConta.CARTAO_CREDITO` continua existindo no `account-service` — não é
removido, não quebra nada da fatia 1 — mas fica sem um fluxo rico de
fatura/parcelamento associado. Serve só pra quem quer anotar manualmente
"tenho uma dívida de cartão" como saldo negativo simples, sem usar o
`card-service` de verdade.

## Consequências

- Dois lugares "sabem" sobre cartão de crédito conceitualmente (o enum
  `TipoConta` e o `card-service`), mas sem sobreposição real de
  responsabilidade — `account-service` nunca calcula fatura/parcela,
  `card-service` nunca gerencia saldo de conta genérica.
- `card-service` ganha seu próprio ciclo de vida e banco, consistente com
  ADR-0001 — sem acoplamento direto entre os bancos, só chamada HTTP
  síncrona no momento de pagar a fatura.
- Se isso confundir o usuário no front-end (dois jeitos de "ter um
  cartão"), revisitar com ADR novo — possivelmente depreciando
  `TipoConta.CARTAO_CREDITO` nesse momento. Não antecipar essa decisão
  agora, sem evidência de que é um problema real.
