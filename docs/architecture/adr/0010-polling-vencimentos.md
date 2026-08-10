# ADR-0010: Verificação de vencimento via job de polling diário, não evento Kafka

Status: Aceita
Data: 2026-08-06

## Contexto

`notification-service` precisa alertar o usuário quando uma despesa
recorrente ou fatura de cartão está perto de vencer (PRD 3.6). O padrão já
estabelecido no projeto pra integração entre serviços é evento assíncrono via
Kafka (ver `overview.md` seção 8/9) — mas eventos representam fatos que já
aconteceram ("transação criada"), e "vencimento daqui a 3 dias" não é um
fato que acontece num instante, é uma projeção calculável a qualquer momento
a partir de uma regra recorrente ainda ativa ou de uma fatura já emitida.

## Decisão

`notification-service` roda um job agendado (Quarkus Scheduler, diário) que:
1. Consulta `transaction-service` (`GET /transacoes-recorrentes/proximos-vencimentos`,
   endpoint interno role `service`) por regras `DESPESA` `ATIVA` com próxima
   ocorrência dentro da janela de antecedência de cada usuário.
2. Consulta `card-service` de forma análoga pra faturas (quando esse serviço
   existir — roadmap #2; endpoint a especificar nessa fatia).
3. Para cada vencimento próximo ainda não alertado (dedup via histórico
   próprio), dispara notificação nos canais habilitados do usuário.

## Consequências

- Acoplamento direto (REST síncrono, role `service`) entre `notification-service`
  e `transaction-service`/`card-service`, em vez de assíncrono — aceitável
  porque é uma consulta de leitura periódica, não uma cadeia de efeitos que
  precise desacoplamento.
- `transaction-service` (e depois `card-service`) precisa expor um endpoint
  de leitura cross-tenant (todos os usuários, não filtrado por token de um
  usuário específico) — só acessível com role `service`, nunca exposto ao
  front-end (mesma regra de ADR-0003 pra endpoints internos).
- Se o volume de usuários crescer muito, esse polling diário e completo pode
  não escalar — revisitar (ex: paginação, ou os serviços de origem
  publicando evento quando uma regra recorrente é criada/alterada, e
  `notification-service` mantendo sua própria projeção local das próximas
  datas). Não implementar essa complexidade agora.
