# Sistema de Finanças Pessoais

[![CI](https://github.com/wep1980/wepdev-financas/actions/workflows/ci.yml/badge.svg)](https://github.com/wep1980/wepdev-financas/actions/workflows/ci.yml)

Sistema de controle financeiro pessoal e multi-usuário, construído com
arquitetura de microsserviços, usando as tecnologias mais adotadas hoje no
mercado. Permite gerenciar contas/receitas/despesas, importar fatura de
cartão e extrato bancário automaticamente (PDF/CSV), e conversar com uma IA
em linguagem natural sobre a própria situação financeira (ex: "quanto tenho
disponível pra gastar esse mês?"). Projeto desenvolvido de forma incremental,
por fatias verticais de funcionalidade, com engenharia de software assistida
por IA (specs, ADRs, roadmap e tasks como fonte da verdade — ver abaixo).

> **Contexto para trabalhar neste repo com IA:** leia [`CLAUDE.md`](CLAUDE.md)
> primeiro — é o índice de toda a documentação viva do projeto (produto,
> arquitetura, decisões, roadmap, tarefas). Este README é só a porta de
> entrada rápida.

## Stack

- **Back-end**: Java 21 + Quarkus
- **Front-end web**: Next.js (React) — App Router também assume o papel de BFF (ver [ADR-0006](docs/architecture/adr/0006-nextjs-frontend-bff.md))
- **Mobile**: React Native
- **Bancos de dados**: MySQL (dados transacionais), Redis (cache), MongoDB
  (dados não estruturados: logs, notificações, histórico de IA)
- **Mensageria**: Apache Kafka
- **Identidade**: Keycloak (OAuth2/OIDC)
- **Segredos**: HashiCorp Vault (entra em fase posterior)
- **Observabilidade**: Prometheus, Grafana, OpenTelemetry
- **Orquestração/deploy**: Docker Compose no desenvolvimento; K3s + Traefik
  no servidor de produção e Argo CD para sincronização declarativa a partir
  do repositório GitOps ([ADR-0030](docs/architecture/adr/0030-deploy-kubernetes-argocd.md))
- **Ingress (produção)**: Cloudflare Tunnel, sem porta pública aberta ([ADR-0019](docs/architecture/adr/0019-ingress-cloudflare-tunnel.md))
- **CI/CD**: GitHub Actions em runner hospedado, imagens imutáveis no GHCR e
  deploy GitOps pelo Argo CD ([ADR-0030](docs/architecture/adr/0030-deploy-kubernetes-argocd.md))
- **IA**: agente orquestrador + RAG para interação em linguagem natural com
  o sistema (`ai-service`); provedor de LLM plugável por usuário (OpenAI ou
  Ollama, ver [ADR-0002](docs/architecture/adr/0002-abstracao-provedor-llm.md));
  Qdrant como vector store ([ADR-0005](docs/architecture/adr/0005-vector-store-qdrant.md))

## Arquitetura

Microsserviços organizados por domínio, cada um com seu próprio banco
(*database-per-service*), comunicação síncrona via REST para operações que
precisam de resposta imediata, e eventos assíncronos via Kafka para
integração entre domínios. Detalhe completo, incluindo lista de serviços
planejados e fluxos, em [`docs/architecture/overview.md`](docs/architecture/overview.md).
Diagramas (contexto, containers, modelo de domínio, classes, implantação) em
[`docs/architecture/diagrams.md`](docs/architecture/diagrams.md).

```
wepdev-financas/
├── CLAUDE.md                   # índice da documentação viva do projeto
├── docker-compose.yml          # infraestrutura completa para desenvolvimento local
├── docs/
│   ├── product/prd.md          # visão, personas, casos de uso, requisitos
│   ├── architecture/           # overview, diagramas, estratégia de IA, testes, ADRs
│   ├── specs/                  # contratos OpenAPI de cada serviço (spec-driven)
│   ├── postman/                # environment e guia de teste manual via Postman
│   ├── roadmap.md              # fatias verticais, em ordem, com status
│   ├── tasks.md                # backlog detalhado da fatia atual
│   └── historico.md            # log cronológico do que já foi pedido/decidido
├── infra/keycloak/             # realm pré-configurado (roles, clients, usuário de teste)
└── services/
    ├── account-service/        # Quarkus — contas financeiras (ver seção Endpoints abaixo)
    ├── transaction-service/    # Quarkus — transações, chama account-service síncrono
    ├── card-service/           # Quarkus — cartões, fatura e parcelamento
    ├── document-service/       # Quarkus — upload/parsing de fatura via LLM local
    ├── budget-service/         # Quarkus — orçamento e "disponível pra gastar"
    ├── ai-service/             # Quarkus — chat com IA, RAG, ação por comando
    └── web/                    # Next.js — front-end, BFF pros seis serviços acima
```

### Estado atual

**`account-service`** está com CRUD completo: criar/listar/buscar/atualizar/
excluir conta (exclusão lógica), débito e crédito de saldo (endpoint interno
pro `transaction-service` consumir), Bean Validation com erro estruturado,
`usuarioId` sempre extraído do token (nunca aceito do cliente — fecha um
IDOR real), evento Kafka em criação, migração Flyway, 45 testes (domínio +
use case + integração `@QuarkusTest`), imagem Docker validada.

**`transaction-service`** registra, lista, edita, cancela e resume por
categoria transações, e gerencia regras de transação recorrente (salário,
assinatura, aluguel — distinto de parcelamento de cartão, ver ADR-0009),
funcionando ponta a ponta: registrar/editar/cancelar chamam o
`account-service` de forma síncrona (débito/crédito, delta na edição, e o
inverso ao cancelar) antes de mudar o próprio estado (sem transação
"fantasma" se a chamada falhar); cancelar é idempotente. Regra recorrente
gera a 1ª ocorrência na criação e as seguintes via job agendado
(`quarkus-scheduler`). Evento Kafka em `transacao.eventos`, migração
Flyway, 91 testes, imagem Docker validada. Backlog do serviço completo
(ver `docs/tasks.md`).

**`card-service`** gerencia cartões de crédito, fatura e parcelamento,
independente de `TipoConta.CARTAO_CREDITO` do `account-service`
([ADR-0022](docs/architecture/adr/0022-card-service-independente-de-conta.md)):
todo cartão tem um `contaPagamentoId` — referência lógica pra uma conta
`CORRENTE`/`POUPANCA`/`CARTEIRA` que paga a fatura, confirmada de forma
síncrona contra o `account-service`. CRUD de cartão; lançar compra (à
vista ou parcelada — parcelas distribuídas automaticamente em faturas
consecutivas, criadas sob demanda, arredondamento absorvido na última
parcela); listar/buscar fatura; pagar fatura (síncrono, idempotente);
job agendado fecha fatura vencida automaticamente. 82 testes, imagem
Docker validada. Backlog do serviço completo (ver `docs/tasks.md`).

**`document-service`** faz upload e parsing de fatura de cartão em PDF via
LLM local (Ollama, ADR-0002) — extrai lançamentos candidatos, usuário
revisa e confirma (nada vira transação sem confirmação explícita, PRD
3.2), evento Kafka pro `transaction-service` criar a `Transacao` de
verdade. Upload é **assíncrono**
([ADR-0024](docs/architecture/adr/0024-upload-documento-assincrono.md)) —
extração via LLM local pode levar minutos pra fatura grande, cliente
sonda o status. Posse da `contaId` (pra saber onde debitar) é confirmada
uma única vez, no momento da confirmação
([ADR-0025](docs/architecture/adr/0025-confirmacao-posse-conta-antes-do-evento.md)).
Testado com faturas reais de três bancos diferentes (Santander, Itaú,
Nubank — cada um com formato de data e estrutura próprios). Extrato,
boleto e ingestão por foto (mobile) ficam pra uma fatia futura. 100+
testes (incluindo um teste de integração que publica evento Kafka real),
imagem Docker validada. Backlog completo (ver `docs/tasks.md`).

**`budget-service`** calcula orçamento por categoria/mês e "disponível
pra gastar" (PRD 3.3) — cruza `account-service` (saldo), `card-service`
(fatura em aberto) e `transaction-service` (despesa recorrente, gasto por
categoria) de forma síncrona, sempre propagando o token do próprio
usuário, nenhuma chamada precisa de posse de um id específico (regra
exata do cálculo e o porquê de cada parcela em
[ADR-0026](docs/architecture/adr/0026-regra-calculo-disponivel-para-gastar.md)).
Resposta devolve o detalhamento item a item de cada parcela (contas,
faturas, despesas recorrentes), não só o total — dá pra auditar de onde
veio o número. 49 testes, imagem Docker validada. Backlog completo (ver
`docs/tasks.md`).

**`ai-service`** é o chat com IA (PRD 3.4/3.5) — um único endpoint
(`POST /chat`) atende pergunta nova, ação, correção e confirmação; o
agente orquestrador decide a intenção a partir do texto + estado da
conversa. Consulta cruza `budget-service` (números exatos) e busca
semântica via RAG (Qdrant, indexado automaticamente a cada
`transacao.eventos` publicado pelo `transaction-service`); ação (ex:
criar despesa) sempre propõe antes de persistir, com confirmação
explícita e expiração em 10 minutos (ADR-0007) — `contaId` nunca é
extraído pelo LLM, sempre resolvido de forma determinística contra o
`account-service`. Primeiro serviço sem MySQL (só MongoDB + Qdrant);
provedor de LLM (OpenAI ou Ollama) é configurável por usuário, `apiKey`
criptografada em repouso (AES-256/GCM). 64 testes, imagem Docker
validada. Backlog completo (ver `docs/tasks.md`).

**`web`** é o front-end Next.js (React) — dashboard com gráfico de gastos
por categoria (PRD 3.7), CRUD completo de conta/transação/cartão, upload
de documento (polling assíncrono até a extração terminar) e chat com a
IA, tudo autenticado via Keycloak (Auth.js v5, client público `web-app`,
sem `client_secret` — [ADR-0027](docs/architecture/adr/0027-autenticacao-frontend-authjs-keycloak.md)).
Server Components/Route Handlers do próprio Next.js fazem o papel de BFF,
propagando o token do usuário logado pros seis serviços acima — nunca
client_credentials. Rodando como container, o servidor Next.js fala com
o Keycloak por dois caminhos diferentes: `AUTH_KEYCLOAK_ISSUER` (público,
só pro redirect de login/logout que o navegador precisa alcançar) e
`AUTH_KEYCLOAK_INTERNAL_ISSUER` (rede interna do Docker, pras chamadas de
troca/renovação de token — discovery OIDC desligado de propósito, senão
o discovery document do Keycloak sempre devolveria URL pública,
inalcançável de dentro do container). 52 testes, imagem Docker validada.
Backlog completo (ver `docs/tasks.md`).

Repositório no GitHub: [`wep1980/wepdev-financas`](https://github.com/wep1980/wepdev-financas)
(privado). O CI executa `mvn test` nos seis serviços Java e
`npm test` no `web`, todos no runner hospedado, cobertura publicada como
artefato do run (JaCoCo nos serviços Java), a imagem Docker de cada um é
validada a cada mudança e, depois dos gates, publicada no GHCR. O scan de
vulnerabilidade usa OWASP Dependency-Check
(ADR-0017) nos serviços Java, `npm audit` no `web` (sem ferramenta Java
lá, ver `security.md` seção 6). O Dependency-Check usa o acesso público à
NVD e cache compartilhado, sem API key do projeto (ver
[`docs/architecture/security.md`](docs/architecture/security.md)). Ver
[`docs/tasks.md`](docs/tasks.md) pro detalhe do que falta em cada item.

`docker compose up -d --build account-service transaction-service
card-service document-service budget-service ai-service web` sobe os
sete serviços + toda a infra (MySQL, Redis, MongoDB, Qdrant, Keycloak,
Kafka, Prometheus, Grafana) do zero, com autenticação de verdade
funcionando — validado ponta a ponta (login no navegador → criar conta →
registrar transação/criar cartão/importar fatura/conversar com a IA →
saldo atualizado, via containers). Pra desenvolvimento do dia a dia, mais
rápido rodar só a infra via compose e os serviços em `mvn quarkus:dev`/
`npm run dev` local (ver README de cada serviço).

**Ollama não roda mais neste `docker-compose.yml`** (ADR-0029,
2026-08-11) — CPU sem GPU não aguentava chat e extração de fatura ao
mesmo tempo (achado real, testado na prática). Agora roda no servidor de
produção, numa GPU dedicada; `document-service`/`ai-service` apontam pra
lá via `OLLAMA_SERVER_URL` (`.env`, nunca versionado). Ver
`.env.example` pra como voltar a apontar pra um Ollama local se
necessário.

## Endpoints principais (`account-service`, porta `8081`)

Contrato completo, com schemas de request/response, em
[`docs/specs/account-service.yaml`](docs/specs/account-service.yaml) — a
tabela abaixo é só um resumo de navegação.

| Método | Path | Role (OIDC) | O que faz |
|---|---|---|---|
| `POST` | `/api/v1/contas` | `usuario` | Cria conta financeira (dono = usuário do token) |
| `GET` | `/api/v1/contas` | `usuario` | Lista contas ativas do usuário autenticado |
| `GET` | `/api/v1/contas/{id}` | `usuario` | Busca conta por id (404 se não existir ou não for sua) |
| `PUT` | `/api/v1/contas/{id}` | `usuario` | Atualiza nome/instituição (404 se não for sua) |
| `DELETE` | `/api/v1/contas/{id}` | `usuario` | Exclui logicamente (inativa; 404 se não for sua) |
| `POST` | `/api/v1/contas/{id}/debitos` | `service` | **Interno** — debita saldo (422 se insuficiente); chamado pelo `transaction-service` |
| `POST` | `/api/v1/contas/{id}/creditos` | `service` | **Interno** — credita saldo; chamado pelo `transaction-service` |

`usuarioId` nunca é um parâmetro de request — é sempre extraído do claim
`sub` do token (dono do recurso = quem está autenticado). Conta de outro
usuário responde 404, igual a não existir, pra não confirmar existência a
quem não é dono (evita IDOR). Role `usuario` é do usuário final (login
normal); role `service` é só pra comunicação serviço-a-serviço via client
credentials, nunca exposta ao front-end (multi-tenancy e roles detalhados
em [ADR-0003](docs/architecture/adr/0003-multi-tenancy-keycloak.md)).

## Endpoints principais (`transaction-service`, porta `8082`)

Contrato completo em [`docs/specs/transaction-service.yaml`](docs/specs/transaction-service.yaml).

| Método | Path | Role (OIDC) | O que faz |
|---|---|---|---|
| `POST` | `/api/v1/transacoes` | `usuario` | Registra transação (receita/despesa) — chama o `account-service` de forma síncrona antes de persistir; 404 se a conta não for sua, 422 se saldo insuficiente |
| `GET` | `/api/v1/transacoes` | `usuario` | Lista transações do usuário autenticado — filtros opcionais `contaId`, `inicio`, `fim` |
| `GET` | `/api/v1/transacoes/resumo-por-categoria` | `usuario` | Soma DESPESA confirmada por categoria num período (`inicio`/`fim` obrigatórios); inclui `percentualDoTotal` e comparação com o período anterior de mesma duração; 400 se `inicio` depois de `fim` |
| `PUT` | `/api/v1/transacoes/{id}` | `usuario` | Edita descrição/valor/categoria/data (não muda conta nem tipo); se o valor mudou, ajusta o saldo pela diferença numa chamada só ao `account-service`; 404 se não for sua; 422 se já cancelada ou saldo insuficiente |
| `DELETE` | `/api/v1/transacoes/{id}` | `usuario` | Cancela (exclusão lógica) e reverte o efeito no saldo; idempotente; 404 se não for sua; 422 se não der pra reverter (saldo insuficiente) |
| `POST` | `/api/v1/transacoes-recorrentes` | `usuario` | Cria regra recorrente (só `MENSAL` no v1) e gera a 1ª ocorrência imediatamente; `quantidadeOcorrencias` omitida = indefinida |
| `GET` | `/api/v1/transacoes-recorrentes` | `usuario` | Lista regras do usuário autenticado — filtro opcional `status` (`ATIVA`\|`PAUSADA`\|`CANCELADA`\|`CONCLUIDA`) |
| `GET` | `/api/v1/transacoes-recorrentes/proximos-vencimentos` | `service` | [Interno] Próximas ocorrências previstas de todas as regras `ATIVA`, dentro da janela de dias informada (`dias` obrigatório) — consumido pelo futuro `notification-service` (ADR-0010) |
| `GET` | `/api/v1/transacoes-recorrentes/{id}` | `usuario` | Busca uma regra pelo id; 404 se não for sua |
| `DELETE` | `/api/v1/transacoes-recorrentes/{id}` | `usuario` | Cancela a regra (exclusão lógica, idempotente) — não afeta ocorrências já geradas |

`usuarioId` também vem sempre do token, mesmo padrão do `account-service`.
Antes de debitar/creditar, o `transaction-service` confirma que a conta
pertence ao usuário chamando `GET /contas/{id}` do `account-service` **com
o próprio token do usuário repassado** — só então usa um token de serviço
(client credentials) pra aplicar o ajuste no endpoint interno. Detalhe em
[`services/transaction-service/README.md`](services/transaction-service/README.md).

## Endpoints principais (`card-service`, porta `8083`)

Contrato completo em [`docs/specs/card-service.yaml`](docs/specs/card-service.yaml).

| Método | Path | Role (OIDC) | O que faz |
|---|---|---|---|
| `POST` | `/api/v1/cartoes` | `usuario` | Cria cartão — confirma `contaPagamentoId` contra o `account-service`; 404 se não existir/não for sua |
| `GET` | `/api/v1/cartoes` | `usuario` | Lista cartões ativos do usuário autenticado |
| `GET` | `/api/v1/cartoes/{id}` | `usuario` | Busca cartão por id (404 se não existir ou não for seu) |
| `PUT` | `/api/v1/cartoes/{id}` | `usuario` | Atualiza apelido/bandeira/limite/dias/contaPagamentoId (reconfirma posse) |
| `DELETE` | `/api/v1/cartoes/{id}` | `usuario` | Exclui logicamente (inativa; idempotente; não afeta faturas/compras futuras) |
| `POST` | `/api/v1/cartoes/{id}/compras` | `usuario` | Lança compra à vista ou parcelada — distribui as parcelas em faturas consecutivas, criando cada uma sob demanda; 404 se o cartão não for seu |
| `GET` | `/api/v1/cartoes/{id}/faturas` | `usuario` | Lista faturas do cartão, mais recente primeiro — filtro opcional `status` (`ABERTA`\|`FECHADA`\|`PAGA`) |
| `GET` | `/api/v1/faturas/{id}` | `usuario` | Busca fatura com as parcelas que a compõem; 404 se não for sua |
| `POST` | `/api/v1/faturas/{id}/pagar` | `usuario` | Paga a fatura — débito síncrono na `contaPagamentoId` do cartão, idempotente; 422 se ainda `ABERTA` ou saldo insuficiente |
| `GET` | `/api/v1/faturas/proximos-vencimentos` | `service` | [Interno] Faturas `FECHADA` com vencimento dentro da janela de dias informada (`dias` obrigatório) — consumido pelo futuro `notification-service` (ADR-0010) |

`usuarioId` vem sempre do token, mesmo padrão dos outros dois serviços.
Detalhe da chamada síncrona ao `account-service` em
[`services/card-service/README.md`](services/card-service/README.md).

## Endpoints principais (`document-service`, porta `8084`)

Contrato completo em [`docs/specs/document-service.yaml`](docs/specs/document-service.yaml).

| Método | Path | Role (OIDC) | O que faz |
|---|---|---|---|
| `POST` | `/api/v1/documentos` | `usuario` | Upload de fatura em PDF (multipart) — responde **202** na hora (status `RECEBIDO`), extração roda em background |
| `GET` | `/api/v1/documentos` | `usuario` | Lista documentos do usuário autenticado — filtro opcional `status` |
| `GET` | `/api/v1/documentos/{id}` | `usuario` | Busca documento com os lançamentos extraídos — é esse que o cliente sonda (polling) até o status sair de `PROCESSANDO` |
| `POST` | `/api/v1/documentos/{id}/confirmar` | `usuario` | Confirma lançamentos selecionados (`contaId` + ids) — confirma posse da conta, publica evento Kafka, idempotente |

`usuarioId` vem sempre do token. Diferente dos outros serviços, o upload é
assíncrono (ADR-0024) — a resposta imediata nunca tem os lançamentos, só o
GET subsequente. Detalhe completo (formatos de fatura suportados,
integração com `account-service`/`transaction-service`) em
[`services/document-service/README.md`](services/document-service/README.md).

## Endpoints principais (`budget-service`, porta `8085`)

Contrato completo em [`docs/specs/budget-service.yaml`](docs/specs/budget-service.yaml).

| Método | Path | Role (OIDC) | O que faz |
|---|---|---|---|
| `POST` | `/api/v1/orcamentos` | `usuario` | Cria orçamento por categoria/mês; 422 se já existe um ativo pra essa categoria nesse mês |
| `GET` | `/api/v1/orcamentos` | `usuario` | Lista orçamentos ativos do mês (`mes=AAAA-MM` obrigatório) com `valorConsumido`/`valorDisponivel` calculados na hora |
| `PUT` | `/api/v1/orcamentos/{id}` | `usuario` | Atualiza o `valorLimite`; 404 se não for seu |
| `DELETE` | `/api/v1/orcamentos/{id}` | `usuario` | Cancela (exclusão lógica, idempotente); 404 se não for seu |
| `GET` | `/api/v1/reserva` | `usuario` | Busca a reserva atual (nunca 404 — valor 0 se nunca definida) |
| `PUT` | `/api/v1/reserva` | `usuario` | Define/atualiza a reserva (valor único, não é por mês) |
| `GET` | `/api/v1/disponivel-para-gastar` | `usuario` | Calcula "disponível pra gastar" no mês (`mes=AAAA-MM` obrigatório) — regra exata em ADR-0026 |

`usuarioId` vem sempre do token. Todas as chamadas de saída (account-service/
card-service/transaction-service) propagam o token do próprio usuário —
nenhum endpoint interno (`role: service`) é usado aqui, diferente de
`card-service`/`transaction-service`. Detalhe completo em
[`services/budget-service/README.md`](services/budget-service/README.md).

## Endpoints principais (`ai-service`, porta `8086`)

Contrato completo em [`docs/specs/ai-service.yaml`](docs/specs/ai-service.yaml).

| Método | Path | Role (OIDC) | O que faz |
|---|---|---|---|
| `POST` | `/api/v1/chat` | `usuario` | Endpoint único do chat — pergunta, ação, correção ou confirmação, decidido pelo agente a partir do texto + estado da conversa; `conversaId` omitido inicia conversa nova |
| `GET` | `/api/v1/conversas` | `usuario` | Lista conversas do usuário autenticado, mais recente primeiro (só resumo) |
| `GET` | `/api/v1/conversas/{id}` | `usuario` | Busca conversa com histórico completo de mensagens; 404 se não for sua |
| `GET` | `/api/v1/configuracao` | `usuario` | Busca configuração de IA do usuário (nunca 404; `apiKey` nunca volta na resposta) |
| `PUT` | `/api/v1/configuracao` | `usuario` | Define provedor de LLM (`OPENAI` com `apiKey`, ou `OLLAMA`); 400 se `apiKey` ausente com `OPENAI` |

`usuarioId` vem sempre do token. Ação (ex: criar transação) nunca é
persistida na mesma chamada que a propõe — exige confirmação explícita do
usuário (ADR-0007), e `contaId` é sempre resolvido de forma determinística
contra o `account-service`, nunca extraído pelo LLM. Detalhe completo em
[`services/ai-service/README.md`](services/ai-service/README.md).

## URLs úteis (ambiente de dev local)

Depois de `docker compose up -d` (infra) + `mvn quarkus:dev` em cada
serviço. Lista completa de toda interface gráfica disponível (dev **e** o
padrão pra produção, ainda não implantado) em
[`docs/architecture/interfaces-graficas.md`](docs/architecture/interfaces-graficas.md)
— a tabela abaixo é só o resumo do dia a dia:

| O quê | URL | Credenciais |
|---|---|---|
| `web` (front-end) | `http://localhost:3000` | login via Keycloak (Auth.js), ver `usuario.teste` em `infra/keycloak/realm-financas.json` |
| `account-service` (REST) | `http://localhost:8081` | token OIDC, ver abaixo |
| `account-service` — Swagger UI | `http://localhost:8081/q/swagger-ui` | — |
| `account-service` — OpenAPI (importar no Postman) | `http://localhost:8081/q/openapi` | — |
| `account-service` — health | `http://localhost:8081/q/health` | — |
| `transaction-service` (REST) | `http://localhost:8082` | token OIDC, ver abaixo |
| `transaction-service` — Swagger UI | `http://localhost:8082/q/swagger-ui` | — |
| `card-service` (REST) | `http://localhost:8083` | token OIDC, ver abaixo |
| `card-service` — Swagger UI | `http://localhost:8083/q/swagger-ui` | — |
| `document-service` (REST) | `http://localhost:8084` | token OIDC, ver abaixo |
| `document-service` — Swagger UI | `http://localhost:8084/q/swagger-ui` | — |
| `budget-service` (REST) | `http://localhost:8085` | token OIDC, ver abaixo |
| `budget-service` — Swagger UI | `http://localhost:8085/q/swagger-ui` | — |
| `ai-service` (REST) | `http://localhost:8086` | token OIDC, ver abaixo |
| `ai-service` — Swagger UI | `http://localhost:8086/q/swagger-ui` | — |
| Keycloak (admin console) | `http://localhost:8080` | `admin` / `admin` |
| Keycloak (token, realm `financas`) | `http://localhost:8080/realms/financas/protocol/openid-connect/token` | ver `infra/keycloak/realm-financas.json` |
| Grafana | `http://localhost:3001` | `admin` / `admin` |
| Kafka UI ([kafka-ui](https://github.com/provectus/kafka-ui) — tópicos, mensagens, config) | `http://localhost:8090` | — |
| Prometheus | `http://localhost:9090` | — |
| MySQL (`account_db`/`transaction_db`/`card_db`/`document_db`/`budget_db`, ex: via DBeaver) | `localhost:3307` | `financas` / `financas` |
| MongoDB (`document_service` + `ai-service`, ex: via MongoDB Compass) | `localhost:27017` | `financas` / `financas` |
| Kafka (broker, ex: DBeaver/cliente Kafka) | `localhost:29092` | — |
| Ollama (LLM, ADR-0002/ADR-0029) | Servidor de produção, `OLLAMA_SERVER_URL` no `.env` | — (não roda mais local, ver ADR-0029) |
| Qdrant (vetores, ADR-0005) — dashboard web | `http://localhost:6333/dashboard` | — |
| Qdrant — REST/gRPC | `http://localhost:6333` / `localhost:6334` | — |

Todas as credenciais acima são só de dev, nunca as mesmas em produção (ver
[`docs/architecture/security.md`](docs/architecture/security.md)). Guia
completo de teste manual (Postman, fluxo de token `usuario` e `service`) em
[`docs/postman/README.md`](docs/postman/README.md).

## Diagramas

Vivos em [`docs/architecture/diagrams.md`](docs/architecture/diagrams.md) —
contexto, containers/serviços, modelo de domínio, **diagrama de classes**
(atualizado junto com cada classe de domínio nova) e implantação em
produção. Prévia publicada dos diagramas de classe atuais (`Conta` e
`Transacao`): https://claude.ai/code/artifact/9dc61745-3325-4874-b9f5-589126f57b00

## Roadmap

Fatias verticais, em ordem, com status — ver
[`docs/roadmap.md`](docs/roadmap.md). Trabalho detalhado da fatia atual em
[`docs/tasks.md`](docs/tasks.md).
