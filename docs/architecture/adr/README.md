# Architecture Decision Records

Uma decisão de arquitetura relevante = um arquivo novo aqui, nunca edição de
ADR antigo (se a decisão mudou, cria um ADR novo que supera o anterior e
referencia ele). Template:

```markdown
# ADR-000N: Título curto

Status: Proposta | Aceita | Superada por ADR-000X
Data: AAAA-MM-DD

## Contexto
Qual problema/trade-off motivou a decisão.

## Decisão
O que foi decidido, de forma direta.

## Consequências
O que isso custa/facilita, o que fica mais difícil, o que revisitar se X
mudar.
```

## Índice

| ADR | Título | Status |
|---|---|---|
| [0001](0001-arquitetura-microsservicos.md) | Microsserviços em vez de monólito | Aceita |
| [0002](0002-abstracao-provedor-llm.md) | Abstração de provedor de LLM (OpenAI/Ollama) | Aceita |
| [0003](0003-multi-tenancy-keycloak.md) | Multi-tenancy via Keycloak/OIDC desde o início | Aceita |
| [0004](0004-prioridade-ingestao-documentos.md) | Prioridade de ingestão: fatura PDF e extrato PDF/CSV | Aceita |
| [0005](0005-vector-store-qdrant.md) | Qdrant como vector store para RAG | Proposta |
| [0006](0006-nextjs-frontend-bff.md) | Next.js como front-end web, assumindo o papel de BFF | Aceita |
| [0007](0007-confirmacao-obrigatoria-acoes-ia.md) | Confirmação obrigatória para ações de IA que alteram dado | Aceita |
| [0008](0008-voz-transcrita-no-dispositivo.md) | Voz transcrita no dispositivo, não server-side | Aceita |
| [0009](0009-transacao-recorrente-vs-parcelamento-cartao.md) | Transação recorrente distinta de parcelamento de cartão | Aceita |
| [0010](0010-polling-vencimentos.md) | Verificação de vencimento via polling diário, não evento Kafka | Aceita |
| [0011](0011-push-firebase-cloud-messaging.md) | Push notification via Firebase Cloud Messaging | Aceita |
| [0012](0012-whatsapp-biblioteca-nao-oficial.md) | WhatsApp via biblioteca não-oficial, risco assumido | Aceita |
| [0013](0013-email-provedor-transacional.md) | E-mail via provedor transacional | Proposta |
| [0014](0014-boleto-financiamento-linha-digitavel.md) | Boleto de financiamento no escopo; parsing via linha digitável | Aceita |
| [0015](0015-ingestao-foto-visao-llm.md) | Ingestão de foto via visão do LLM, sem OCR separado | Proposta |
| [0016](0016-topologia-producao-servidor-unico.md) | Produção em servidor Linux único, Docker Compose + reverse proxy | Aceita — reverse proxy substituído por ADR-0019 |
| [0017](0017-gestao-vulnerabilidade-dependencias.md) | Gestão de vulnerabilidade de dependências (Dependabot + gate no CI) | Aceita |
| [0018](0018-cicd-github-actions.md) | CI/CD via GitHub Actions (build, teste, scan, deploy por SSH) | Aceita — deploy substituído por ADR-0020 |
| [0019](0019-ingress-cloudflare-tunnel.md) | Ingress via Cloudflare Tunnel | Aceita |
| [0020](0020-deploy-runner-self-hosted.md) | Deploy via runner self-hosted do GitHub Actions | Aceita — refinada por ADR-0021 |
| [0021](0021-deploy-kamal.md) | Deploy dos serviços de aplicação via Kamal (zero-downtime, rollback) | Aceita |
