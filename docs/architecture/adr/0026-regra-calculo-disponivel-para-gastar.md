# ADR-0026: Regra de cálculo de "disponível pra gastar" no budget-service

Status: Aceita
Data: 2026-08-10

## Contexto

O PRD (seção 3.3) define a fórmula em alto nível: "disponível pra gastar
esse mês" = saldo em conta corrente/carteira − compromissos já sabidos do
mês (contas fixas, fatura de cartão em aberto) − reserva definida pelo
usuário — mas deixa a regra exata pro `budget-service` decidir e documentar
"quando implementada". Chegou a hora: item 1 da fatia 4 é a spec, e a spec
não dá pra escrever sem fechar essa regra primeiro (os endpoints e o
payload de resposta dependem diretamente do que entra no cálculo).

O cálculo cruza dado de três serviços diferentes, síncrono, dentro de uma
única requisição HTTP do usuário (mesmo padrão de propagação de token já
usado em `document-service`→`account-service`, ADR-0025):

- **Saldo de contas** (`account-service`, `GET /api/v1/contas`): soma do
  `saldo` das contas ativas do usuário com `tipo` `CORRENTE` ou
  `CARTEIRA` — são as únicas que representam dinheiro líquido disponível
  agora (poupança/investimento não entram: resgatar tem fricção/efeito
  colateral que o v1 não modela; cartão de crédito é dívida, não saldo
  disponível).
- **Fatura de cartão em aberto** (`card-service`, `GET /api/v1/cartoes` +
  `GET /api/v1/cartoes/{id}/faturas?status=FECHADA`): soma do `valorTotal`
  das faturas com status **`FECHADA`** (não `PAGA`) com `dataVencimento`
  dentro do mês consultado. Fatura `ABERTA` fica de fora de propósito — seu
  `valorTotal` ainda não é definitivo (compras continuam entrando até o
  fechamento), então não é um "compromisso já sabido" no sentido do PRD,
  é uma estimativa em movimento.
- **Despesas recorrentes ativas** ("contas fixas", `transaction-service`,
  `GET /api/v1/transacoes-recorrentes?status=ATIVA`): soma do `valor` de
  toda regra `TipoTransacao.DESPESA` ativa cuja `dataInicio` já começou
  (`dataInicio <= último dia do mês consultado`). **Não** tenta prever se a
  ocorrência do mês já foi gerada/paga pelo `transaction-service` — trata
  cada regra ativa como um compromisso mensal fixo de valor constante,
  ponto, independente do timing interno de geração de ocorrência.
- **Reserva**: valor único definido pelo usuário no próprio
  `budget-service` (não é por mês — é um valor fixo que o usuário ajusta
  quando quiser, ex: "sempre quero manter R$500 de colchão").

## Decisão

```
disponivelParaGastar(mês) =
    Σ saldo das contas CORRENTE/CARTEIRA (account-service)
  − Σ valorTotal das faturas FECHADA com vencimento no mês (card-service)
  − Σ valor das despesas recorrentes ATIVA com dataInicio <= fim do mês (transaction-service)
  − reserva (budget-service)
```

A resposta do endpoint (`GET /api/v1/disponivel-para-gastar`) devolve não
só o número final, mas o **detalhamento de cada parcela** (lista de contas
com saldo, lista de faturas em aberto, lista de despesas recorrentes) —
requisito direto do PRD seção 6 ("a IA deveria conseguir explicar de onde
tirou o número"): se a resposta fosse só o total, nem a IA nem o usuário
teriam como auditar o cálculo.

Motivo de **não** tentar prever a ocorrência exata de despesa recorrente
do mês (em vez de aproximar "regra ativa = 1 compromisso por mês"): o
`transaction-service` não documenta (nem foi implementado ainda, até onde
se sabe nessa sessão) um contrato estável de "quando exatamente a N-ésima
ocorrência é materializada como `Transacao`". Duplicar esse cálculo de
data no `budget-service` sem esse contrato documentado seria depender de
comportamento interno não garantido — mais frágil que a aproximação
simples, que também é o modelo mental mais natural pro usuário ("meu
aluguel é um compromisso de R$X todo mês", independente de o sistema já
ter lançado a transação daquele mês especificamente ou não).

## Consequências

- `budget-service` chama `account-service`, `card-service` e
  `transaction-service` de forma síncrona, propagando o token do usuário
  (mesmo padrão de dois tokens do ADR-0025) — três integrações de saída
  novas, nenhuma delas grava nada nos outros serviços, só leitura.
- Poupança e investimento nunca entram no "disponível pra gastar" — se
  isso se mostrar errado na prática (usuário achar contraintuitivo), é
  mudança de regra de produto, revisitar via novo ADR, não silenciosamente
  no código.
- Se o `transaction-service` um dia ganhar um contrato explícito de
  previsão de próxima ocorrência (ex: um campo `proximaOcorrenciaEm` na
  `TransacaoRecorrenteResponse`), vale revisitar essa aproximação pra usar
  o dado real em vez da suposição "1 por mês" — não é a decisão definitiva
  pra sempre, é a mais simples que não depende de contrato inexistente.
- Orçamento por categoria (`POST/GET/PUT/DELETE /api/v1/orcamentos`) é uma
  funcionalidade separada do "disponível pra gastar" — usa
  `transaction-service` `GET /transacoes/resumo-por-categoria` (endpoint já
  existente, mesmo cálculo do dashboard/IA) pra saber quanto já foi gasto
  numa categoria/mês, comparado ao `valorLimite` definido pelo usuário. As
  duas funcionalidades compartilham o serviço mas não compartilham cálculo.
