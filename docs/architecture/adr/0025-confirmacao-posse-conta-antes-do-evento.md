# ADR-0025: Posse da conta é confirmada no document-service, antes do evento — o consumer Kafka não reverifica

Status: Aceita
Data: 2026-08-09

## Contexto

Ao implementar o item 9 (consumer Kafka no `transaction-service` pro tópico
`documento.lancamentos-confirmados`), apareceu um problema real: o padrão já
estabelecido pra debitar/creditar conta (`AccountServiceClient.debitar()`
em `transaction-service`, usado por `RegistrarTransacaoUseCase`) confirma
posse da conta chamando `GET /api/v1/contas/{id}` do `account-service` **com
o token do usuário autenticado, propagado do request HTTP recebido**
(`PropagarAutorizacaoHeadersFactory`, reusa o 404 do account-service como
gate de autorização — mesmo padrão em card-service e transaction-service).

Um consumer Kafka não tem requisição HTTP em andamento — não existe token
de usuário pra propagar. E `GET /api/v1/contas/{id}` no account-service é
`@RolesAllowed("usuario")` só (não aceita role `service`) — não dá pra
simplesmente trocar pelo cliente interno de service-account, porque esse
endpoint especificamente não permite esse role.

## Decisão

A verificação de posse (`contaId` pertence a `usuarioId`) acontece **uma
única vez**, no `document-service`, dentro de
`ConfirmarLancamentosUseCase` — que roda numa requisição HTTP síncrona,
autenticada, com o token do usuário disponível pra propagar (mesmo padrão
de dois tokens já usado em card-service: `AccountServiceUsuarioClient` +
`PropagarAutorizacaoHeadersFactory`). Se a conta não existir ou não
pertencer ao usuário, a confirmação falha com 404 **antes** de qualquer
evento ser publicado.

O consumer Kafka no `transaction-service` **não reverifica posse** — ele
recebe `usuarioId` e `contaId` já no payload do evento (que só foi
publicado depois da verificação acima) e vai direto pro débito/crédito via
cliente de serviço (`AccountServiceInternoClient`, role `service`,
endpoints `/debitos` e `/creditos`). Isso exigiu um método novo na porta
`AccountServiceClient` do transaction-service —
`debitarSemConfirmarPosse`/`creditarSemConfirmarPosse` — que pula a etapa
de `GET /contas/{id}` (que nem teria token pra usar) e vai direto pro
ajuste de saldo.

## Consequências

- `document-service` ganhou sua própria integração com `account-service`
  (porta `AccountServiceClient`, só com `confirmarPosseDaConta`) — não
  tinha nenhuma até agora. Não é integração com `card-service` (isso
  continua fora de escopo, ADR-0023), é com `account-service` mesmo,
  mesmo padrão dos outros serviços.
- Fica uma responsabilidade implícita importante: **qualquer novo produtor
  do tópico `documento.lancamentos-confirmados`** (ou de um evento
  parecido no futuro) precisa fazer essa verificação de posse ele mesmo,
  antes de publicar — o consumer nunca vai fazer isso. Documentar isso no
  README do transaction-service pra não virar um gap de segurança
  esquecido se um segundo produtor aparecer.
- Sem verificação de posse no consumer significa que, se o evento for
  publicado com um `contaId`/`usuarioId` incoerente (bug ou, pior,
  Kafka comprometido), o débito acontece sem checagem — aceitável pro MVP
  porque o tópico é interno (não exposto a cliente externo) e o único
  produtor hoje (document-service) já valida antes de publicar.
