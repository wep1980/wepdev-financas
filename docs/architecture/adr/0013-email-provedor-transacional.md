# ADR-0013: E-mail via provedor transacional (proposta)

Status: Proposta (não confirmada pelo usuário — decisão minha, revisar)
Data: 2026-08-06

## Contexto

Alerta de vencimento (PRD 3.6) precisa de canal e-mail. Diferente do
WhatsApp (ADR-0012), aqui não há trade-off de risco de banimento — é só
escolher como enviar e-mail de forma confiável (entregabilidade, não cair em
spam).

## Decisão (proposta)

Usar um provedor de e-mail transacional (ex: AWS SES, SendGrid, ou outro
equivalente — a escolha específica do provedor fica em aberto, não é uma
decisão estrutural) via SMTP ou API, em vez de um servidor SMTP próprio
(entregabilidade própria é difícil de manter — cai em spam com facilidade
sem reputação de IP estabelecida).

## Consequências

- Depende de credencial de um provedor externo (API key ou credencial SMTP),
  guardada como segredo (Vault), nunca hardcoded.
- Tem custo por volume acima do free tier do provedor escolhido — irrelevante
  no volume esperado (uso pessoal/poucos usuários).
- **Ação pendente**: confirmar o provedor específico com o usuário antes de
  implementar a fatia de `notification-service` — está listado como
  proposta, não decisão fechada (mesmo tratamento dado a ADR-0005/Qdrant).
