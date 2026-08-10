# ADR-0023: Escopo da primeira fatia do `document-service` — sem integração com card-service

Status: Aceita
Data: 2026-08-09

## Contexto

`document-service` (fatia 3, ver `docs/tasks.md`) começa pela ingestão de
fatura de cartão em PDF. Existe uma ambiguidade de design: uma fatura de
cartão importada poderia, em teoria, virar `Parcela`s dentro do modelo de
`Fatura`/`Cartao` já existente no `card-service` (fatia 2) — afinal é
literalmente o mesmo tipo de documento. Alternativa: ignorar esse paralelo e
tratar cada lançamento da fatura importada como uma transação avulsa comum
(`Transacao` tipo `DESPESA`) no `transaction-service`, exatamente como o
diagrama de sequência já documentado em `docs/architecture/overview.md`
seção 3 descreve (`document-service → Kafka → transaction-service → débito
síncrono no account-service`).

Também é preciso escolher uma biblioteca de extração de texto de PDF — não
documentado em nenhum ADR anterior.

## Decisão

1. **Sem integração com `card-service` nessa fatia.** Cada lançamento
   confirmado de uma fatura importada vira uma `Transacao` avulsa comum via
   o fluxo já desenhado em `overview.md` seção 3 — não cria/atualiza
   `Cartao`/`Fatura`/`Parcela` nenhum. Motivo: essa integração não está
   especificada em lugar nenhum (nem PRD, nem `overview.md`, nem ADR-0009,
   que já discute recorrência vs. parcelamento mas não fala de importação de
   fatura), e criaria acoplamento novo entre dois serviços que hoje só se
   comunicam via `account-service`. Se o usuário validar a fatia e quiser
   esse cruzamento (ex: "documento importado de fatura deveria reconciliar
   com o cartão que já cadastrei"), isso vira uma fatia própria com ADR
   dedicado — não decidir agora por especulação.
2. **Extração de texto de PDF via Apache PDFBox** (`org.apache.pdfbox:pdfbox`).
   Motivo: licença Apache 2.0 (compatível), biblioteca madura e a mais
   adotada do ecossistema Java pra essa tarefa (ver princípio de escolha de
   dependência em `CLAUDE.md`), sem dependência de binário externo (ao
   contrário de rotas que usam Tesseract/poppler via processo externo).

## Consequências

- MVP da fatia 3 fica mais simples de implementar e testar — só usa
  primitivas que já existem (`Transacao` avulsa via evento Kafka), sem
  desenhar um relacionamento novo entre agregados de dois serviços
  diferentes.
- Fica uma lacuna consciente: hoje, se o usuário importa uma fatura de
  cartão que ele **também** cadastrou no `card-service`, os dois fluxos são
  paralelos e não se enxergam (a fatura importada não fecha/paga a `Fatura`
  do `card-service`). Isso é aceitável pro MVP mas deve ser revisitado
  quando as duas fatias estiverem em uso real.
- PDFBox entra como dependência nova de `services/document-service/pom.xml`
  — igual às demais libs do projeto, sujeita ao gate de CVE do CI
  (`dependency-check`, ADR-0017).
