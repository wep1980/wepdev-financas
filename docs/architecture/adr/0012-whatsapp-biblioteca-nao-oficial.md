# ADR-0012: Canal WhatsApp via biblioteca não-oficial, risco assumido

Status: Aceita
Data: 2026-08-06

## Contexto

PRD 3.6 pede alerta de vencimento por WhatsApp. Duas vias possíveis: API
oficial (Meta Cloud API / via BSP como Twilio) — exige verificação de
negócio, aprovação de template de mensagem, e tem custo por conversa; ou uma
biblioteca não-oficial (ex: Baileys, whatsapp-web.js) que se conecta a um
número de WhatsApp pessoal via protocolo não documentado do WhatsApp Web,
sem custo e sem processo de aprovação.

O usuário escolheu explicitamente a via não-oficial, ciente do trade-off:
mais simples e sem custo, mas **viola os Termos de Uso do WhatsApp** e tem
risco real do número usado ser banido pela Meta a qualquer momento, sem
aviso prévio.

## Decisão

`notification-service` envia mensagem de WhatsApp usando uma biblioteca
não-oficial (ex: Baileys), conectada a um número de WhatsApp pessoal
dedicado a isso (não o número pessoal do dia a dia do usuário, pra limitar o
impacto se o número for banido). Escopo aceito conscientemente por ser
projeto de uso pessoal/poucos usuários — não é uma decisão que se sustenta
num produto com muitos usuários reais.

## Consequências

- **Risco de banimento é real e não mitigável tecnicamente** — é a natureza
  de usar um protocolo não suportado oficialmente. `notification-service`
  não pode ser a única forma de o usuário saber de um vencimento — push e
  e-mail (ADR-0011, ADR-0013) continuam funcionando independente do
  WhatsApp (ver PRD seção 4).
- A sessão da biblioteca não-oficial (login via QR code) precisa ser
  mantida/renovada manualmente de tempos em tempos — não é "configura uma
  vez e esquece" como uma API oficial com token de longa duração.
- Número de WhatsApp usado fica dedicado a isso — não compartilhar com uso
  pessoal do dono do número.
- **Gatilho pra revisitar este ADR**: se o sistema ganhar usuários reais além
  do dono do projeto, ou se o número for banido mais de uma vez, migrar pra
  API oficial (Meta Cloud API) — nesse ponto o custo/processo de aprovação
  deixa de ser desproporcional ao valor.
