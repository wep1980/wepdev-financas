# Arquitetura — Visão Geral

> Como o sistema é composto. Porquês de decisões específicas ficam nos ADRs
> (`docs/architecture/adr/`); isso aqui é o mapa.

## 1. Estilo arquitetural

Microsserviços por domínio, *database-per-service*, comunicação síncrona
(REST) quando a resposta é necessária imediatamente e precisa de consistência
forte, eventos assíncronos (Kafka) para propagar mudanças de domínio entre
serviços que não precisam de resposta imediata. Ver ADR-0001 para o porquê de
microsserviços em vez de monólito nesse projeto.

## 2. Serviços

| Serviço | Responsabilidade | Porta | Banco | Status |
|---|---|---|---|---|
| `account-service` | Contas financeiras (criar, listar, debitar/creditar saldo) | 8081 | MySQL (`account_db`) | ✅ Entregue — CRUD completo + débito/crédito, 45 testes, CI verde |
| `transaction-service` | Registrar/consultar transações (inclusive recorrentes, ver ADR-0009); chama `account-service` p/ refletir no saldo | 8082 | MySQL (`transaction_db`) | ✅ Entregue — registrar/listar/editar/cancelar/resumo/recorrentes, 91 testes, CI verde |
| `card-service` | Cartões de crédito, faturas, parcelamento — independente de `TipoConta.CARTAO_CREDITO` (ADR-0022) | 8083 | MySQL (`card_db`) | ✅ Entregue — CRUD de cartão + fatura/parcelamento/pagamento, 82 testes, CI verde |
| `document-service` | Upload e parsing de fatura de cartão (PDF) via LLM local (Ollama, ADR-0002); extrato (PDF/CSV) e boleto de financiamento (ADR-0014) ficam pra fatias seguintes; no mobile, foto entra também depois (ADR-0015); gera lançamentos pendentes, usuário confirma, evento Kafka vira transação de verdade (ADR-0023) | 8084 | MongoDB (documento bruto + resultado do parsing) + MySQL (lançamentos pendentes) | ✅ Entregue (fatura PDF) — upload assíncrono + extração LLM + confirmação + evento, 100+ testes, CI verde |
| `budget-service` | Orçamento por categoria/mês, cálculo de "disponível pra gastar" — cruza account-service/card-service/transaction-service de forma síncrona (ADR-0026) | 8085 | MySQL (`budget_db`) | ✅ Entregue — orçamento + disponível pra gastar, 49 testes, CI verde |
| `ai-service` | Orquestração de agentes, RAG, chat em linguagem natural, MCP tools | 8086 | Qdrant (vetores, proposto ADR-0005) + MongoDB (histórico de conversas) | Planejado |
| `notification-service` | Alertas de vencimento (despesa recorrente, fatura de cartão) via push/WhatsApp/e-mail; preferências de notificação por usuário | 8087 | MongoDB | Planejado |

Cada serviço tem seu contrato em `docs/specs/<nome-do-servico>.yaml`, escrito
**antes** do código (spec-driven). Não há mais um "BFF/gateway web" como
serviço separado — esse papel foi absorvido pelo Next.js (ver ADR-0006).

## 2.1 Clientes

| Cliente | Framework | Fala com os serviços via | Entrada de comando IA |
|---|---|---|---|
| Web | Next.js (React) | Server Components/Route Handlers do próprio Next.js, que agregam os microsserviços (papel de BFF) — ver ADR-0006 | Só texto |
| Mobile | React Native | Chama os microsserviços diretamente (sem BFF — não há processo Next.js no dispositivo). Se a agregação client-side ficar repetitiva entre web e mobile, revisitar com um ADR (ex: gateway HTTP dedicado) — não antecipar essa complexidade agora | Texto ou voz (transcrita no dispositivo, nunca enviada como áudio — ADR-0008) |

UX, identidade visual, tipografia e paleta de cores ainda não foram
definidos — ficam para quando começarmos a fatia de front-end (roadmap #6).

## 3. Fluxo: ingestão de documento → transação

```mermaid
sequenceDiagram
    participant U as Usuário (front-end)
    participant D as document-service
    participant A as account-service
    participant K as Kafka
    participant T as transaction-service

    U->>D: POST /documentos (upload fatura.pdf)
    D-->>U: 202, status RECEBIDO
    D->>D: processamento em background (PDFBox + LLM, ADR-0024)
    U->>D: GET /documentos/{id} (polling)
    D-->>U: status AGUARDANDO_CONFIRMACAO + lançamentos
    U->>D: POST /documentos/{id}/confirmar (contaId + ids selecionados)
    D->>A: confirma posse de contaId (síncrono, ADR-0025)
    D->>K: evento "documento.lancamentos-confirmados"
    D-->>U: 204
    K->>T: consome evento
    T->>A: débito/crédito síncrono (sem reverificar posse — já feita acima, ADR-0025)
    T-->>K: evento "transacao.criada"
```

Ponto chave: o `document-service` nunca cria transação sozinho — ele produz
lançamentos candidatos, o usuário confirma, e só aí o fluxo já conhecido
(`transaction-service` → `account-service` síncrono) roda. Isso evita
"transação fantasma" vinda de parsing errado.

Upload é **assíncrono** (ADR-0024) — testado na prática que a extração via
LLM local pode levar minutos, não segundos, pra fatura com muitos
lançamentos; segurar isso numa única requisição HTTP síncrona não era
viável. Cliente sonda `GET /documentos/{id}` até o status sair de
`PROCESSANDO`.

Posse de `contaId` (pra saber em qual conta debitar/creditar) é confirmada
**uma única vez**, no `document-service`, síncrono, no momento da
confirmação — é o único ponto do fluxo com o token do usuário disponível
pra propagar pro account-service. O consumer Kafka no `transaction-service`
não tem requisição HTTP em andamento (logo, sem token) e por isso **confia
integralmente** nessa verificação já feita (ADR-0025) — não é dado
opcional, é uma responsabilidade que qualquer produtor futuro desse tópico
precisa assumir.

O diagrama acima mostra upload de PDF; foto (mobile) entra no mesmo ponto —
só muda a estratégia de extração usada dentro do processamento em
background (ADR-0015), o resto do fluxo (confirmação obrigatória, evento,
débito/crédito síncrono) é idêntico.

## 4. Fluxo: comando em linguagem natural (`ai-service`)

Ver `docs/architecture/ai-strategy.md` para o detalhe completo. Dois tipos de
intent, tratados de forma bem diferente:

### 4.1 Consulta (só leitura)

`ai-service` recebe a pergunta, decide (via agente orquestrador) quais dados
precisa buscar (RAG sobre transações/faturas + chamada direta ao
`budget-service` para números exatos), monta contexto, chama o LLM
configurado (OpenAI ou Ollama) e retorna resposta rastreável.

### 4.2 Ação (cria/altera dado)

Exemplo: *"criar uma despesa recorrente de 24 meses no valor de R$19.990"*.

```mermaid
sequenceDiagram
    participant U as Usuário (mobile texto/voz* ou web texto)
    participant AI as ai-service
    participant T as transaction-service

    U->>AI: comando em linguagem natural
    AI->>AI: extrai parâmetros estruturados (agente orquestrador)
    AI-->>U: resumo da ação proposta (nada persistido ainda)
    U->>AI: confirma (ou corrige e recebe novo resumo)
    AI->>T: cria TransacaoRecorrente / Transacao
    T-->>AI: confirmação
    AI-->>U: resumo do que foi criado
```
*voz é transcrita no próprio dispositivo antes de chegar aqui — ver ADR-0008.

Mesmo princípio de confirmação explícita usado no fluxo de documento (seção
3) — nenhuma mutação de dado financeiro acontece sem o usuário confirmar
vendo exatamente o que vai ser criado. Ver ADR-0007.

## 5. Fluxo: alerta de vencimento

```mermaid
sequenceDiagram
    participant J as notification-service (job diário)
    participant T as transaction-service
    participant C as card-service
    participant N as notification-service (envio)
    participant U as Usuário

    J->>T: GET /transacoes-recorrentes/proximos-vencimentos (role=service)
    J->>C: GET faturas próximas do vencimento (role=service)
    J->>J: filtra o que já foi alertado (histórico próprio)
    J->>N: dispara envio por canal habilitado do usuário
    N-->>U: push (som), WhatsApp e/ou e-mail
    N->>N: registra alerta como enviado (dedup)
```

Ver ADR-0010 (por que polling em vez de evento Kafka aqui), ADR-0011 (push/
FCM), ADR-0012 (WhatsApp via biblioteca não-oficial, risco assumido) e
ADR-0013 (e-mail, proposta). Os três canais são independentes — falha num
não afeta os outros (PRD seção 4).

## 6. Fluxo: "disponível pra gastar" (`budget-service`)

```mermaid
sequenceDiagram
    participant U as Usuário (front-end)
    participant B as budget-service
    participant A as account-service
    participant C as card-service
    participant T as transaction-service

    U->>B: GET /disponivel-para-gastar?mes=2026-08
    B->>A: GET /contas (token do usuário)
    A-->>B: contas ativas (saldo por conta)
    B->>C: GET /cartoes + GET /cartoes/{id}/faturas?status=FECHADA
    C-->>B: faturas fechadas de todos os cartões
    B->>T: GET /transacoes-recorrentes?status=ATIVA
    T-->>B: despesas recorrentes ativas
    B->>B: filtra por mês, aplica fórmula (ADR-0026)
    B-->>U: total + detalhamento (contas/faturas/despesas)
```

Três chamadas síncronas de leitura, todas propagando o token do próprio
usuário (mesmo padrão de dois tokens já usado em
`document-service`→`account-service`, ADR-0025) — nenhuma confirma posse
de um id específico, porque os cinco endpoints chamados já filtram pelo
`sub` do token no servidor. O filtro por mês (fatura por
`dataVencimento`, despesa recorrente por `dataInicio`) acontece dentro do
`budget-service`, não nos clientes — `account-service`/`card-service`/
`transaction-service` continuam devolvendo dado bruto, sem saber "qual
mês" foi pedido. Regra exata da fórmula e o porquê de cada parcela:
ADR-0026.

`POST/GET/PUT/DELETE /orcamentos` (orçamento por categoria/mês) segue o
mesmo princípio de leitura síncrona, mas só chama `transaction-service`
(`GET /transacoes/resumo-por-categoria`, endpoint já existente, mesmo
cálculo do dashboard/IA) — não tem diagrama próprio por ser mais simples
que o fluxo acima.

## 7. Multi-tenancy

Todo dado é particionado por `usuarioId`. Autenticação/autorização via
Keycloak (OIDC) — token carrega o `usuarioId` (subject) e roles
(`usuario`/`admin`/`service`). Toda query em todo serviço filtra por
`usuarioId` do token, nunca por parâmetro livre não validado contra o token.
Endpoints internos serviço-a-serviço usam role `service` (client credentials),
nunca expostos ao front-end. Ver ADR-0003.

## 8. Comunicação e consistência

- **Síncrono (REST)**: quando a operação precisa de efeito imediato e
  consistente (ex: transação → débito de saldo). Retry + timeout via SmallRye
  Fault Tolerance cobrem falha transitória de rede.
- **Assíncrono (Kafka)**: para propagação de eventos de domínio que outros
  serviços reagem sem precisar bloquear quem gerou o evento (ex:
  `conta.eventos`, `transacao.eventos`, `documento.eventos`). Formato ainda
  JSON cru; evolução para CloudEvents/Avro + schema registry é item de
  roadmap, não bloqueante.

## 9. Stack completa

Ver tabela no `README.md` raiz — não duplicado aqui pra evitar dessincronia
entre os dois arquivos. Se a stack mudar, atualize os dois.
