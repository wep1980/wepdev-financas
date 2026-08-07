# Roadmap — Fatias Verticais

> Documento vivo. Atualizar status conforme o trabalho acontece. Reordenar
> se prioridade mudar — é normal, só não esquecer de registrar o porquê aqui
> ou num ADR se for uma mudança estrutural.

Ordem pensada pra chegar o mais rápido possível no valor central do produto
(ver PRD): dado real do usuário → importado automaticamente → IA respondendo
perguntas sobre ele. Front-end vem depois de já existir algo real pra
mostrar; mobile depois do web validado.

| # | Fatia | Entrega | Status |
|---|---|---|---|
| 1 | Contas + Transações (back-end) | `account-service` + `transaction-service` funcionando, testados, com fluxo síncrono de débito/crédito | 🔲 Speced (OpenAPI pronto), código não iniciado |
| 2 | `card-service` | Cartões de crédito, fatura e parcelamento, lançamento manual | 🔲 Planejado |
| 3 | `document-service` — parsing manual assistido | Upload de fatura (PDF), extrato (PDF/CSV) e boleto de financiamento (PDF/foto), extração de lançamentos, fluxo de confirmação (PRD 3.2). Foto via mobile depende de confirmar ADR-0015 | 🔲 Planejado |
| 4 | `budget-service` | Orçamento por categoria/mês, cálculo de "disponível pra gastar" (PRD 3.3) | 🔲 Planejado |
| 5 | `ai-service` — RAG + chat | `LlmProvider` (ADR-0002), Qdrant (ADR-0005, confirmar antes), tools MCP, agente orquestrador, responde às perguntas do PRD 3.4 | 🔲 Planejado |
| 6 | Front-end Next.js (React) | Dashboard com gráfico de gastos por categoria (PRD 3.7), CRUD completo de conta/transação, upload de documento, chat com a IA. Definir UX/identidade visual/tipografia/paleta antes ou no início desta fatia (ainda em aberto) | 🔲 Planejado |
| 7 | App React Native | Mesmas funcionalidades do web, mobile-first | 🔲 Planejado |
| 8 | `notification-service` | Alertas de vencimento (despesa recorrente, fatura de cartão) via push/WhatsApp/e-mail — spec já pronta em `docs/specs/notification-service.yaml` | 🔲 Planejado |
| 9 | Deploy em produção | Docker Compose pros dados/infra + Kamal pros serviços de aplicação (zero-downtime, rollback automático — ADR-0021) no servidor Linux único (ADR-0016), ingress via Cloudflare Tunnel (ADR-0019), runner self-hosted do GitHub Actions cuidando do deploy (ADR-0018/0020). Kubernetes/Helm/Terraform/ArgoCD/Istio/Vault viram evolução condicional, não o próximo passo padrão | 🔲 Planejado |

## Por que essa ordem (mudanças em relação ao README original)

- `document-service` e `ai-service` foram **antecipados** em relação ao
  README original (que os deixava por último, depois de front-end/mobile).
  Motivo: a proposta de valor central do produto — ler documento e responder
  pergunta em linguagem natural — é o que diferencia isso de "mais uma
  planilha", então validar isso cedo (mesmo sem front-end bonito, via
  Swagger/Postman) reduz risco do projeto.
- Front-end web só entra na fatia 6, depois de já existir dado real e IA
  funcionando por trás — evita construir tela pra funcionalidade que ainda
  vai mudar de forma.

## Legenda de status

🔲 Planejado · 🔶 Em andamento · ✅ Entregue (testado, documentado)

## Próxima ação concreta

Ver `docs/tasks.md` — fatia 1 (Contas + Transações) é a atual.
