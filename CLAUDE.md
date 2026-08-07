# wepdev-financas — Contexto para IA (Claude Code e afins)

Este arquivo é lido automaticamente no início de cada sessão. Ele é o ponto de
entrada — não repete o que já está em `docs/`, apenas aponta pra lá. Se algo
aqui divergir de `docs/`, **`docs/` manda** (atualize este arquivo depois).

## O que é o projeto

Sistema de finanças pessoais, multi-usuário, que permite:
- gerenciar contas, receitas e despesas;
- ler faturas de cartão de crédito e extratos bancários (PDF/CSV) e transformar
  em transações automaticamente;
- responder perguntas em linguagem natural sobre a situação financeira do
  usuário (ex: "quanto tenho disponível pra gastar esse mês?"), via LLM + RAG
  sobre os dados financeiros do próprio usuário.

Detalhes completos: [`docs/product/prd.md`](docs/product/prd.md).

## Mapa da documentação (leia antes de propor mudanças relevantes)

| Documento | Conteúdo |
|---|---|
| `docs/product/prd.md` | Visão, personas, casos de uso, requisitos funcionais/não-funcionais |
| `docs/architecture/overview.md` | Serviços, portas, bancos, fluxo síncrono/assíncrono, multi-tenancy |
| `docs/architecture/ai-strategy.md` | Abstração de LLM, RAG, agentes de IA, MCP tools |
| `docs/architecture/testing-strategy.md` | Pirâmide de testes, ferramentas, definição de pronto |
| `docs/architecture/adr/` | Decisões de arquitetura, uma por arquivo, com contexto e consequências |
| `docs/architecture/diagrams.md` | Diagrama de contexto, de containers/serviços, modelo de domínio e implantação — visão estrutural do todo |
| `docs/architecture/deployment.md` | Ambiente de dev (Windows) e produção (servidor Linux único do usuário, já com outro site exposto) |
| `docs/architecture/security.md` | Gestão de segredos, inventário de credenciais do sistema, transporte, logging — política central de "nunca expor credencial" |
| `docs/specs/*.yaml` | Contratos OpenAPI de cada serviço (spec-driven: spec antes do código) |
| `docs/roadmap.md` | Fatias verticais, em ordem, com status |
| `docs/tasks.md` | Backlog de tarefas da fatia atual (o que fazer agora, em detalhe) |
| `docs/historico.md` | Log cronológico de tudo que foi pedido, sessão a sessão — pra escanear rápido "o que já foi discutido" |

**Regra de ouro:** decisão de arquitetura relevante → vira ADR. Mudança de
escopo/prioridade → atualiza `roadmap.md`. Início/fim de tarefa → atualiza
`tasks.md`. Serviço/entidade/integração nova → atualiza `diagrams.md`. Toda
sessão em que o usuário pede algo novo → uma entrada nova em `historico.md`.
Sem isso os documentos apodrecem e voltam a ser inúteis.

## Princípios não-negociáveis

1. **Spec-driven**: todo endpoint novo começa com o contrato OpenAPI em
   `docs/specs/` antes da implementação.
2. **SOLID + Clean Code**: classes pequenas, uma responsabilidade, dependências
   injetadas e voltadas a interfaces (portas), não a implementações concretas.
   Isso é especialmente importante no `ai-service` (ver ADR-0002 — provedor de
   LLM é uma porta, nunca acoplar direto a SDK da OpenAI ou do Ollama).
3. **Teste é parte da entrega, não um extra**: nenhuma classe de
   serviço/domínio é considerada pronta sem sua classe de teste correspondente.
   Ver `docs/architecture/testing-strategy.md` para o que testar em cada
   camada. Nenhum PR/tarefa fecha sem teste.
4. **Fatias verticais**: entregamos funcionalidade de ponta a ponta (domínio →
   API → o mínimo de front necessário pra validar), não camadas horizontais
   completas de uma vez. Ver `docs/roadmap.md`.
5. **Exclusão lógica, não física** em entidades financeiras (histórico é
   auditável e não pode sumir).
6. **Dados financeiros são sensíveis**: nunca logar valores/descrições de
   transações em texto claro, em nenhum nível de log; segredos (API keys de
   LLM, credenciais) sempre via variável de ambiente / Vault, nunca hardcoded
   ou commitados. Política completa e inventário de credenciais em
   `docs/architecture/security.md` — sistema seguro e confiável é requisito
   não-negociável, não um "depois a gente vê".
7. **Sem lib com vulnerabilidade conhecida**: Dependabot + gate no CI (OWASP
   Dependency-Check/npm audit/Trivy) barram merge com CVE HIGH/CRITICAL sem
   exceção documentada. Ver ADR-0017 e `docs/architecture/security.md` seção
   6. Ao escolher uma lib nova, preferir a mais adotada/mantida do
   ecossistema — menos superfície de risco, patch mais rápido.

## Convenção de idioma

Domínio de negócio (nomes de campos de API, classes de domínio, eventos
Kafka, tabelas) em **português**, seguindo o que já está em
`docs/specs/*.yaml` (`ContaResponse`, `usuarioId`, `criarConta`, etc.) —
é a linguagem ubíqua do usuário/domínio. Termos técnicos genéricos de
infraestrutura (nomes de pacote, camadas, ex. `repository`, `service`,
`controller`) em inglês, como é convenção no ecossistema Java/Quarkus.
Documentação (`docs/`, ADRs, comentários de PR) em português.

## Stack (resumo — detalhe em `docs/architecture/overview.md`)

Java 21 + Quarkus · Next.js (React) · React Native · MySQL/Redis/MongoDB · Qdrant
(vetores, proposto em ADR-0005) · Kafka · Keycloak (OIDC) · Vault · Prometheus/
Grafana/OTel · Docker Compose (dev e produção) + Kamal pros serviços de
aplicação (ADR-0021) · GitHub Actions. Produção é servidor Linux único, não
cluster — Kubernetes/Helm/Terraform/ArgoCD **não** são o padrão (ADR-0016),
viram evolução condicional se algum dia fizer sentido.

## Estado atual

`account-service` funcional (CRUD de leitura + criação + débito/crédito de
saldo, 28 testes, imagem Docker validada) — falta só CI. `transaction-service`
ainda só tem spec OpenAPI, sem código. Ver `docs/tasks.md` para o detalhe.

## Subagentes de IA para desenvolvimento

Ainda não configuramos subagentes dedicados do Claude Code (`.claude/agents/`)
para este repo. Se fizer sentido mais adiante (ex: um agente que só escreve
specs OpenAPI, outro que só escreve testes de integração Quarkus), avaliamos
sob demanda — não criar por criar.
