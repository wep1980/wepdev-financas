# ADR-0009: Transação recorrente é conceito do `transaction-service`, distinto de parcelamento de cartão

Status: Aceita
Data: 2026-08-06

## Contexto

Dois conceitos parecidos, mas diferentes, surgiram ao detalhar ações de IA
(PRD 3.5): (1) **transação recorrente genérica** — ex: salário mensal, uma
assinatura, ou "despesa recorrente de 24 meses" pedida à IA sem menção a
cartão — que repete um lançamento por conta própria; (2) **parcelamento de
compra no cartão de crédito** — já previsto pro `card-service` desde o
README original — uma compra única dividida em parcelas que aparecem em
faturas sucessivas. Sem separar isso, fica ambíguo onde uma "despesa
recorrente de 24x" deveria viver, e o `card-service` ganharia
responsabilidade sobre recorrência que não tem nada a ver com cartão (ex:
aluguel, salário).

## Decisão

`transaction-service` ganha o conceito de `TransacaoRecorrente`: uma regra
(valor, frequência, quantidade de ocorrências ou indefinida, categoria,
conta) que gera `Transacao`s ao longo do tempo, independente de cartão. Ver
contrato em `docs/specs/transaction-service.yaml`.

`card-service` mantém parcelamento como conceito próprio, atrelado a uma
fatura/compra específica no cartão — **não** reaproveita `TransacaoRecorrente`.

Quando o agente de IA (ai-strategy.md 4.2) interpreta um comando de despesa
recorrente sem menção a cartão, ele cria uma `TransacaoRecorrente` via
`criar_transacao`. Se o usuário mencionar que é uma compra parcelada no
cartão, é responsabilidade do agente direcionar pro fluxo de `card-service`
em vez do genérico — esse roteamento específico será detalhado quando
`card-service` for implementado (roadmap #2), não bloqueia a fatia atual.

## Consequências

- Dois modelos de "repetição de lançamento" na arquitetura (um genérico no
  `transaction-service`, um específico de cartão no `card-service`) —
  decisão consciente pra não forçar uma abstração única prematura entre
  domínios diferentes (cada serviço modela só o que é seu).
- Se isso gerar duplicação real de lógica entre os dois serviços conforme
  `card-service` for implementado, revisitar com ADR novo.
