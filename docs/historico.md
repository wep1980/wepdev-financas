# Histórico de Pedidos e Decisões

> Log cronológico de tudo que foi pedido, sessão a sessão — pra você
> conseguir escanear "o que já foi discutido" sem reler PRD/ADRs inteiros.
> Cada entrada é curta e aponta pros documentos que foram criados/alterados;
> o conteúdo em si mora só lá (PRD, ADRs, overview, specs), nunca duplicado
> aqui. Toda sessão nova vira uma entrada nova, no topo ou no fim — manter
> ordem cronológica.

## 2026-08-06 — Especificação inicial

Pedido: refazer a especificação do zero. Sistema pra gerenciar receitas e
despesas, ler faturas de cartão e extratos bancários, com IA (OpenAI ou
Ollama) via RAG respondendo perguntas em linguagem natural (ex: "quanto
tenho disponível pra gastar esse mês?"). Documentação viva pra não perder
contexto entre sessões. SOLID, clean code, teste em tudo. Engenharia de
software assistida por IA.

Decisões: manter microsserviços (não simplificar pra monólito), abstração de
provedor de LLM desde o início, multi-usuário desde o início, priorizar
fatura PDF e extrato PDF/CSV.

Criado: `CLAUDE.md`, `docs/product/prd.md`, `docs/architecture/overview.md`,
`docs/architecture/ai-strategy.md`, `docs/architecture/testing-strategy.md`,
ADR-0001 a 0005, `docs/roadmap.md`, `docs/tasks.md`.

## 2026-08-06 — Front-end Next.js e mobile

Pedido: front-end web em Next.js (não React puro), app mobile em React
Native. UX/visual/tipografia/paleta ficam pra depois.

Decisões: Next.js (App Router) assume o papel de BFF, elimina o serviço
"BFF/gateway web" que estava planejado separado.

Criado/alterado: ADR-0006; stack e diagrama de clientes atualizados em
`README.md`/`overview.md`.

## 2026-08-06 — Comandos de ação via IA

Pedido: usuário poder falar/escrever comandos que alteram dado, ex: "adicione
uma nova receita mensal de R$10.000" ou "criar uma despesa recorrente de 24
meses no valor de R$19.990". Voz + texto no mobile, só texto no web.

Decisões: toda ação de IA passa por confirmação explícita antes de persistir
(mesmo princípio já usado pra documento importado); voz é transcrita no
próprio dispositivo, nunca chega como áudio no back-end; transação recorrente
é conceito novo do `transaction-service`, separado de parcelamento de cartão.

Criado/alterado: PRD seção 3.5; `ai-strategy.md` seção 4.2 e 6; ADR-0007,
0008, 0009; endpoints `/transacoes-recorrentes*` em
`docs/specs/transaction-service.yaml`; tasks de recorrência em `tasks.md`.

## 2026-08-06 — Alertas de vencimento

Pedido: alertar sobre despesa mensal perto de vencer, por som no celular
(push), WhatsApp e e-mail.

Decisões: WhatsApp via biblioteca não-oficial conectada a número pessoal
(risco de banimento assumido conscientemente — escolha do usuário depois de
eu explicar o trade-off frente à API oficial); push via Firebase Cloud
Messaging; e-mail via provedor transacional (proposta, provedor específico
ainda não escolhido); verificação de vencimento via job de polling diário,
não evento Kafka.

Criado/alterado: PRD seção 3.6; ADR-0010 a 0013;
`docs/specs/notification-service.yaml` (novo); endpoint
`/transacoes-recorrentes/proximos-vencimentos` em `transaction-service.yaml`;
`roadmap.md`.

## 2026-08-06 — Colaboração, CRUD, boleto, foto, dashboard

Pedido: regras de negócio em dúvida decidimos juntos, sugestões minhas são
bem-vindas. No front-end: página de gráfico de gastos por categoria; CRUD
completo (cadastrar/atualizar/excluir) de despesas e receitas; anexar fatura
de cartão, extrato e também boleto de financiamento; no mobile, envio de
documento por foto.

Decisões: exclusão de transação é lógica (cancelamento + reversão de saldo),
nunca física; boleto tem parsing determinístico via linha digitável antes de
cair no caminho por LLM; ingestão por foto usa visão do LLM em vez de OCR
separado (proposta minha, não confirmada).

Criado/alterado: PRD seções 3.1, 3.2, 3.7; ADR-0014, 0015 (status: proposta);
endpoints `PUT`/`DELETE /transacoes/{id}` e
`GET /transacoes/resumo-por-categoria` em `transaction-service.yaml`;
`roadmap.md`, `tasks.md`. Memória salva: colaborar em regras de negócio,
sugestões proativas bem-vindas.

## 2026-08-06 — Diagramas e histórico

Pedido: diagramas principais do sistema, evoluindo conforme a implementação
cresce; um documento único com tudo que já foi pedido (este arquivo).

Criado: `docs/architecture/diagrams.md` (contexto, containers, modelo de
domínio); `docs/historico.md` (este arquivo).

## 2026-08-06 — Ambiente de implantação

Pedido: informar que produção vai ser um servidor Linux único, próprio do
usuário, já hospedando um site de portfólio exposto na web; dev é Windows,
acesso ao servidor via SSH. Docker preferencial, mas serviços nativos (ex.
banco instalado direto) também são opção.

Decisões: produção usa Docker Compose (evoluindo o que já existe), não
Kubernetes — reconsiderado frente à realidade de servidor único (era
K8s/Helm por padrão no roadmap original). Reverse proxy é pré-requisito pra
qualquer coisa exposta, já que o portfólio existente não pode quebrar.
Nenhum dado sensível (IP, domínio, credencial) entra em arquivo versionado.

Criado/alterado: `docs/architecture/deployment.md` (novo); ADR-0016; diagrama
de implantação em `diagrams.md` seção 5; `roadmap.md` #9.

## 2026-08-06 — Segurança: nunca expor credencial

Pedido: reforçar que o sistema nunca deve expor credenciais sensíveis, e
precisa ser seguro e confiável — princípio geral, não uma feature pontual.

Ações: auditei o que já existia no repo e encontrei um risco real —
`docker-compose.yml` tinha senha hardcoded (mysql/mongo/keycloak/grafana) e
não havia `.gitignore` nenhum. Corrigido agora, antes de qualquer `git init`.

Criado/alterado: `.gitignore` (novo, bloqueia `.env`/chaves/certificados);
`.env.example` (novo, documenta variáveis com placeholder, nunca segredo
real); `docker-compose.yml` (credenciais viraram `${VAR:-default}`, mesmo
comportamento em dev, pronto pra receber valor real via `.env`/Vault);
`docs/architecture/security.md` (novo — consolida gestão de segredo,
inventário de credenciais do sistema, transporte, logging); `CLAUDE.md` e
PRD atualizados apontando pra lá.

## 2026-08-06 — Evitar libs vulneráveis

Pedido: evitar usar bibliotecas com vulnerabilidades conhecidas.

Decisões: Dependabot (nativo do GitHub, cobre Maven/npm/Docker) monitora e
abre PR de atualização; CI ganha gate que falha em vulnerabilidade HIGH/
CRITICAL sem exceção documentada — OWASP Dependency-Check (Java), `npm
audit` (Node), Trivy (imagem Docker). Isso cobre CVE catalogada, não
substitui o julgamento sobre lib não-oficial (caso do WhatsApp, ADR-0012),
que continua sendo um risco à parte.

Criado/alterado: ADR-0017; `security.md` seção 6; `CLAUDE.md` princípio 7;
item novo na definição de pronto em `testing-strategy.md`.

## 2026-08-07 — CI/CD via GitHub Actions

Pedido: usar GitHub Actions no CI/CD, junto das ferramentas mais usadas no
mercado (dúvida inicial foi sobre GitOps/ArgoCD — descartado por ora porque
é ferramenta pensada pra Kubernetes, e produção é servidor único via Docker
Compose, ADR-0016; confirmado que a intenção era GitHub Actions mesmo).

Decisões: CI roda em todo PR (build + teste + scan de vulnerabilidade,
ADR-0017), com path filter por serviço. CD roda só no merge pra `main`:
imagem vai pro GitHub Container Registry (`ghcr.io`, integra nativo sem
credencial extra), deploy por SSH no servidor único (ADR-0016) rodando
`docker compose pull && up -d`. Chave SSH de deploy é dedicada (não a
pessoal do usuário), guardada como GitHub Actions Secret.

Criado/alterado: ADR-0018; inventário de credenciais em `security.md`;
`deployment.md` seção 5 (novo); diagrama de implantação em `diagrams.md`
atualizado; `roadmap.md` #9; task de CI (sem o D ainda) na fatia 1 em
`tasks.md`.

## 2026-08-07 — Levantamento real do servidor

Pedido: o usuário rodou os comandos de reconhecimento no servidor de
verdade (Ubuntu 24.04, 12 vCPU/15GB RAM, Docker já instalado) e colou a
saída — revelou fatos que invalidavam parte do que eu tinha assumido em
ADR-0016/ADR-0018: o host já roda vários outros projetos (portfólio, Umami,
Postgres, pgAdmin, Portainer, Watchtower), acesso público é via **Cloudflare
Tunnel** (não port-forward/Nginx), e SSH só aceita conexão da VPN WireGuard
do usuário — um runner hospedado do GitHub não alcançaria.

Decisões (perguntei, já que eram trade-off real de acesso/infra): ingress
via Cloudflare Tunnel, igual ao portfólio; deploy via runner self-hosted do
GitHub Actions instalado no próprio servidor, em vez de SSH direto.

Criado/alterado: `deployment.md` reescrito com dado real (seções 2-6);
ADR-0019 (ingress) e ADR-0020 (deploy), ambos superando detalhes de
ADR-0016/ADR-0018 (status das duas atualizado, corpo mantido como registro
histórico); inventário de credenciais em `security.md` ajustado; diagrama de
implantação em `diagrams.md` refeito; `roadmap.md` #9.

## 2026-08-07 — Deploy com rollback automático (Kamal)

Pedido: o usuário já tinha usado, em outros projetos, um esquema onde a
imagem nova substitui a antiga automaticamente mas a anterior fica de
standby caso a nova dê erro — perguntou se dava pra usar essa solução de
novo, aceitando sugestão de ferramenta de mercado com a mesma finalidade.

Decisão: **Kamal** (recomendei, usuário confirmou) — zero-downtime real
(container novo só recebe tráfego depois de passar no healthcheck via
`kamal-proxy`), rollback com um comando. Escopo: só os serviços de
aplicação stateless (account-service, transaction-service, front-end
Next.js); bancos e infra continuam via `docker-compose` normal (ADR-0016),
já que blue-green não se aplica a banco de dados. Kamal roda a partir do
runner self-hosted (ADR-0020), sem exigir nada novo de rede.

Criado/alterado: ADR-0021 (refina ADR-0020); `deployment.md` seção 6;
diagrama de implantação em `diagrams.md`; inventário de credenciais em
`security.md`; requisito de healthcheck endpoint em `testing-strategy.md`;
`roadmap.md` #9.

## 2026-08-07 — Início da implementação (account-service)

Pedido: começar a construir o sistema de verdade.

Feito: scaffold do `account-service` (Quarkus 3.38.1, Java 21) via
`quarkus-maven-plugin`, com extensions rest/rest-jackson/hibernate-orm-panache/
jdbc-mysql/flyway/hibernate-validator/oidc/smallrye-fault-tolerance/
messaging-kafka/smallrye-health/opentelemetry. Primeira fatia vertical
completa: **criar conta** — domínio (`Conta`, regra de saldo), caso de uso
(`CriarContaUseCase`), persistência (JPA separado do domínio + Panache +
migração Flyway), evento Kafka (`conta.eventos`), endpoint REST
(`POST /api/v1/contas`, seguindo `docs/specs/account-service.yaml`), e 12
testes (domínio, use case com Mockito, integração `@QuarkusTest` — Dev
Services do Quarkus provisionou MySQL e Kafka automaticamente pro teste,
sem precisar configurar Testcontainers manualmente). Todos os testes
passando (`mvn test`, exit 0).

Pendências registradas em `docs/tasks.md`: casos de uso restantes (listar,
buscar, debitar, creditar), endpoints GET, e o `docker-compose.yml` precisa
apontar pro `Dockerfile.jvm` gerado pelo Quarkus (hoje aponta pra um
`Dockerfile` que não existe).

Criado/alterado: `services/account-service/` (projeto novo);
`docs/tasks.md` (checklist atualizado com o que foi feito);
`docs/architecture/overview.md` (status do serviço na tabela).

## 2026-08-07 — Continuação: fecha o back-end de `account-service`

Pedido: "vamos retornar de onde paramos" — continuação direta da sessão
anterior, sem pedido novo de escopo.

Feito: casos de uso restantes do `account-service` — listar contas ativas
por usuário (`ListarContasUseCase`), buscar por id (`BuscarContaUseCase`,
404 via `ContaNaoEncontradaException`), debitar/creditar saldo
(`DebitarSaldoUseCase`/`CreditarSaldoUseCase`). Endpoints REST
`GET /api/v1/contas` e `GET /api/v1/contas/{id}` (role `usuario`).
Endpoints internos novos `POST /api/v1/contas/{id}/debitos` e `/creditos`
(role `service`, ADR-0003) pro `transaction-service` reusar depois —
especificados em `account-service.yaml` antes de implementar
(spec-driven). `docker-compose.yml` corrigido pra apontar o build do
`account-service` pro `Dockerfile.jvm` gerado pelo Quarkus (validado com
`docker build` isolado). README do serviço atualizado.

Decisão registrada (sem ADR formal, baixo risco/reversível — ver memória de
colaboração): endpoints de ajuste de saldo não têm idempotência ainda —
retry do `transaction-service` (SmallRye Fault Tolerance, ainda não
implementado) pode em tese duplicar um ajuste se a falha ocorrer depois do
saldo mudar mas antes da resposta chegar. Risco aceito por ora, fica pra
revisar quando o `transaction-service` implementar o retry de verdade.

19 → 28 testes, todos passando (`mvn test`, exit 0).

Criado/alterado: `docs/specs/account-service.yaml` (endpoints internos +
`AjusteSaldoRequest`); código novo em
`services/account-service/src/main/java/.../application` e
`infrastructure/rest`; `docker-compose.yml`;
`services/account-service/README.md`; `docs/tasks.md`;
`docs/architecture/overview.md`.

## 2026-08-07 — Validação real no Postman + confirma Maven

Pedido: confirmar Maven como gerenciador de dependências (já era o caso,
Quarkus usa Maven desde o scaffold) e garantir que dá pra testar tudo que
foi construído no Postman — não só via `@QuarkusTest`.

Feito: subi o `account-service` de verdade (`mvn quarkus:dev`) contra a
infra real do `docker-compose` (MySQL, Kafka, Keycloak) e validei o fluxo
completo via curl/Postman (criar conta → listar → buscar → debitar com role
`service` → 403 debitando com role `usuario`). Encontrei e corrigi dois
problemas reais de ambiente de dev que bloqueavam isso:

1. `quarkus:dev` tentava provisionar sozinho um stack de observabilidade
   (Grafana LGTM, imagem de 600MB) via Dev Services, porque o
   `docker-compose.yml` ainda não tem serviço `otel-collector` — travava o
   boot. Desabilitado em dev (`quarkus.observability.lgtm.enabled=false`)
   até o collector existir de verdade.
2. O usuário de teste do Keycloak (`realm-financas.json`) não tinha
   `firstName`/`lastName` — o Keycloak 25 exige esses campos no user
   profile por padrão, e sem eles o grant de senha falha com "Account is
   not fully set up" (erro genérico, não óbvio). Corrigido no realm.

Também adicionei ao realm um mapeamento de service account
(`transaction-service` → role `service`) só pra dar pra testar os
endpoints internos de débito/crédito antes do `transaction-service`
existir de verdade — documentado em `docs/postman/README.md` (seção nova,
fluxo Client Credentials).

Criado/alterado: `services/account-service/src/main/resources/application.properties`
(observabilidade); `infra/keycloak/realm-financas.json` (user profile +
service account); `docs/postman/README.md` (seção 4, testar role
`service`); `docs/postman/financas-dev.postman_environment.json`
(variáveis do client `transaction-service`); `docs/tasks.md`.

## 2026-08-07 — Diagrama de classes + README do projeto

Pedido: criar diagramas de classe pra entender as regras de negócio, e um
README do projeto com o que o sistema faz, endpoints principais e URLs
úteis. Confirmado nessa conversa: 1 usuário pode ter N contas (sem limite),
sem restrição de unicidade em `usuario_id`.

Feito: diagrama de classe do domínio `Conta` (escopo combinado com o
usuário: só `account-service`, só domínio — sem use cases/ports). Usuário
disse explicitamente que quer o diagrama atualizado junto com cada classe
nova daqui pra frente (memória salva). README raiz reescrito: tirei
Kubernetes/Helm/Terraform/ArgoCD do stack (desatualizado desde
ADR-0016/0021, mesmo desalinhamento corrigido no `CLAUDE.md`), adicionei
tabela de endpoints principais e URLs úteis de dev.

Criado/alterado: `docs/architecture/diagrams.md` seção 4 (nova);
`README.md` (reescrito); `CLAUDE.md` (stack/estado atual corrigidos).
Memória salva: manter diagrama de classes atualizado junto com código novo.

## 2026-08-07 — CRUD completo de conta + fecha falha de IDOR

Pedido: implementar atualização e exclusão de conta, com validação de
campo (id inexistente, usuário inexistente, obrigatórios) e tratamento de
exceção — sugestão do usuário de usar Bean Validation, que já era o padrão
do projeto.

Decisão discutida com o usuário: "usuário não existe" expôs uma falha real
— `POST /contas` aceitava `usuarioId` no corpo sem checar contra o token,
então dava pra criar/listar/buscar conta em nome de qualquer um (IDOR).
Opções apresentadas: extrair `usuarioId` do token (fecha a falha, muda
contrato já validado no Postman) ou manter como estava. Usuário escolheu
fechar a falha.

Feito: `Conta.atualizar(nome, instituicao)` no domínio (tipo/saldo/dono não
editáveis); `AtualizarContaUseCase`/`ExcluirContaUseCase` (exclusão lógica
via `inativar()`, já existia); todo caso de uso que lê/muta uma conta
específica agora recebe `usuarioId` e trata "conta de outro usuário" igual
a "não existe" (404 nos dois casos — nunca 403, pra não confirmar
existência a quem não é dono). `usuarioId` passou a vir do claim `sub` do
JWT (`SecurityIdentity` + `JsonWebToken`, extensão `quarkus-smallrye-jwt`
adicionada), nunca mais de campo de request — `CriarContaRequest` perdeu o
campo `usuarioId`, `GET /contas` perdeu o query param. Endpoints novos:
`PUT /api/v1/contas/{id}`, `DELETE /api/v1/contas/{id}` (204, idempotente).
Exception mappers estruturados: `ConstraintViolationExceptionMapper` (400
com campo+mensagem por violação), mais corpo de mensagem em
`ContaNaoEncontradaExceptionMapper`/`SaldoInsuficienteExceptionMapper`.

Testes de integração precisaram de `quarkus-test-security-jwt`
(`@JwtSecurity`/`@Claim`) pra simular o claim `sub` do token sob
`@TestSecurity` — sem isso não dá pra testar código que lê claim de JWT.
28 → 45 testes, todos passando.

Criado/alterado: `docs/specs/account-service.yaml` (PUT/DELETE,
`AtualizarContaRequest`, `ErroResponse`/`ErroValidacaoResponse`, remove
`usuarioId` do `CriarContaRequest`); código novo/alterado em
`domain`/`application`/`infrastructure/rest` do `account-service`;
`pom.xml` (+`quarkus-smallrye-jwt`, +`quarkus-test-security-jwt` teste);
`docs/architecture/diagrams.md` seção 4.1; `docs/tasks.md`.

## 2026-08-07 — Guia manual de Postman + Kafka UI

Pedido: o usuário já personalizou a collection do Postman e não quer
reimportar a cada mudança de contrato — pediu um jeito de saber o que
ajustar manualmente. Depois perguntou como testar o Kafka, e por fim se
dava pra ver o Kafka funcionando via interface gráfica com capacidade de
configurar (não só visualizar).

Feito: validei manualmente o fluxo Kafka de ponta a ponta (criar conta via
API → evento `ContaCriadaEvento` aparece em `conta.eventos` via
`kafka-console-consumer` dentro do container). Depois adicionei
**kafka-ui** (Provectus) ao compose — Kafdrop foi descartado por ser só
leitura; kafka-ui permite criar/editar tópico, mudar config, produzir
mensagem de teste. Confirmado rodando (`http://localhost:8090`), cluster
online, `readOnly: false`.

Criado: `docs/postman/mudancas-manuais.txt` (guia de "o que mexer na
collection à mão" a cada mudança de contrato, atualizado junto com o
código dali pra frente — memória salva). `docker-compose.yml` (serviço
`kafka-ui` novo, porta `8090:8080`); `README.md` (URLs úteis).

## 2026-08-07 — Início do `transaction-service`: registrar transação

Pedido: "vamos seguir com o próximo passo do nosso sistema" — perguntei se
era `transaction-service` ou fechar CI primeiro; usuário escolheu
`transaction-service` (fecha a fatia 1 de ponta a ponta).

Decisão tomada sem perguntar (baixo risco, aplicando padrão já decidido
com o usuário pro `account-service`): apliquei o mesmo princípio de
"`usuarioId` sempre do token" na spec do `transaction-service` antes de
implementar — removi `usuarioId` de `CriarTransacaoRequest` e dos query
params de listagem/resumo/recorrentes.

Feito: scaffold Quarkus completo (mesma estrutura de camadas). Domínio
`Transacao` (nasce sempre `CONFIRMADA`, porque o efeito no saldo já
aconteceu quando `criar()` roda). Caso de uso **registrar transação**:
chama o `account-service` de forma síncrona (débito/crédito) antes de
persistir — decisão de design nova aqui: duas chamadas com dois tokens
diferentes. `GET /contas/{id}` repassando o **token do próprio usuário**
(`PropagarAutorizacaoHeadersFactory`, um `ClientHeadersFactory` do
MicroProfile Rest Client) confirma que a conta é dele, reusando o 404 do
`account-service` como gate de autorização em vez de reimplementar a
checagem; só depois `POST /contas/{id}/debitos|creditos` com **token de
serviço** (client_credentials, client `transaction-service`, role
`service`) aplica o ajuste de verdade.

Problema real encontrado e corrigido: o token de serviço não estava sendo
anexado nas chamadas internas (401 silencioso) — a extensão certa pro
REST client novo (`quarkus-rest-client-jackson`) é
`quarkus-rest-client-oidc-filter` com `@RegisterProvider(OidcClientRequestReactiveFilter.class)`,
não `quarkus-oidc-client-filter` (nem existe nessa versão) nem a
combinação `quarkus-oidc-client` + `@OidcClientFilter` sozinha (essa
última é do pacote errado, não registra o filtro pro REST client
reativo). Só descobri isso ativando log de debug do REST client
(`quarkus.rest-client.*.logging.scope=request-response`) e vendo que o
header `Authorization` simplesmente não saía na requisição.

Validado de ponta a ponta com os dois serviços reais rodando (não só
mock): criar conta (saldo 500) → registrar despesa de 120 → saldo vira
380 de verdade; saldo insuficiente → 422, saldo não muda; conta de outro
usuário → 404; evento `TransacaoRegistradaEvento` aparece em
`transacao.eventos` via `kafka-console-consumer`. `docker compose up -d
--build account-service transaction-service` sobe os dois do zero.

Criado/alterado: `services/transaction-service/` (projeto novo);
`docs/specs/transaction-service.yaml` (usuarioId do token em todo
endpoint, `ErroResponse`/`ErroValidacaoResponse`); `docker-compose.yml`
(Dockerfile.jvm + `TRANSACTION_SERVICE_CLIENT_SECRET`); `.env.example`;
`docs/architecture/diagrams.md` seção 4.2 (novo); `docs/architecture/overview.md`;
`README.md`; `docs/tasks.md`.

## 2026-08-07 — Listar transações + bug de autenticação via Docker

Pedido: "continue da parte que você ache mais importante primeiro" — sem
mais contexto do usuário nessa sessão; escolhi **listar transações**
porque era a lacuna mais óbvia (dava pra criar transação mas não pra
consultar de volta) e pré-requisito natural pra editar/cancelar depois.

Feito: `ListarTransacoesUseCase` + `GET /api/v1/transacoes`, sempre do
usuário autenticado, filtros opcionais `contaId`/`inicio`/`fim`.

No meio da validação de ponta a ponta, achei um bug real que não aparecia
nem em `quarkus:dev` nem nos testes automatizados: subindo os serviços via
`docker compose up`, TODO request autenticado (mesmo os que já
funcionavam antes) voltava 401. Causa: o Keycloak em modo `start-dev` sem
hostname fixo infere o `issuer` do token pelo Host da requisição — um
token pego via `localhost:8080` (acesso externo, como Postman/navegador
fazem) tem `issuer` diferente do que os serviços esperam quando validam
via rede interna do Docker (`keycloak:8080`). Corrigido fixando
`KC_HOSTNAME=localhost` no Keycloak (issuer vira estável, não importa de
onde o token foi pedido) — só que isso também faz o discovery document do
Keycloak devolver URLs absolutas em "localhost", inalcançáveis de dentro
de um container, então tive que desabilitar discovery
(`quarkus.oidc.discovery-enabled=false`) e apontar os paths manualmente
nos dois serviços (entrada) e no `oidc-client` de saída do
`transaction-service`. Validado com os dois containers reais: criar conta
(saldo 300) → registrar despesa de 50 → saldo 250, com token pego via
`localhost:8080`.

Criado/alterado: `services/transaction-service/.../ListarTransacoesUseCase`,
`TransacaoFiltro`, `TransacaoRepository`/`Impl` (query dinâmica),
`TransacaoResource` (endpoint `GET`); `docker-compose.yml` (`KC_HOSTNAME`);
`account-service` e `transaction-service` `application.properties`
(discovery desabilitado + issuer explícito em `%prod`); `docs/tasks.md`.

## 2026-08-07 — Cancelar transação

Pedido: "de o proximo passo por onde voce ache melhor" — escolhi cancelar
transação (não editar, que é mais complexo — reverter e reaplicar) porque
completa o CRUD básico (criar/listar/cancelar) com escopo contido, mesmo
padrão do que já foi feito.

Feito: `Transacao.cancelar()` no domínio (idempotente — só o chamador
decide se reverte o saldo). `CancelarTransacaoUseCase`: busca a transação
checando posse (mesmo padrão anti-IDOR do `account-service` — 404 pra
inexistente ou de outro usuário), se já `CANCELADA` não faz nada (evita
reverter saldo duas vezes), senão reverte o efeito original no
`account-service` (`DESPESA` credita de volta, `RECEITA` debita de volta)
e só então marca `CANCELADA`. `TransacaoRepository.salvar()` precisou
virar upsert (antes só sabia inserir) pra suportar esse update.

Decisão de regra de negócio sem parar pra perguntar (baixo risco,
documentado): reverter uma `RECEITA` que já foi gasta noutro lugar falha
com 422 (saldo insuficiente pra debitar de volta) e a transação continua
`CONFIRMADA` — comportamento correto (não dá pra "tirar" dinheiro que não
está mais lá), consistente com o resto do sistema (chama account-service
antes de mudar o próprio estado).

Validado de ponta a ponta via containers reais: despesa de 50 numa conta
de 300 → saldo 250 → cancela → saldo volta a 300 → cancela de novo →
continua 300 (idempotência confirmada, não reverte duas vezes).

Criado/alterado: `Transacao.cancelar()`/`isCancelada()`;
`TransacaoNaoEncontradaException`; `CancelarTransacaoUseCase`;
`TransacaoRepository`/`Impl` (`buscarPorId` + `salvar` virou upsert);
`TransacaoResource` (endpoint `DELETE`);
`TransacaoNaoEncontradaExceptionMapper`; `docs/specs/transaction-service.yaml`
(422 documentado no `DELETE`); `docs/tasks.md`.

## 2026-08-07 — Git inicializado + CI (GitHub Actions) + Dependabot

Pedido: "siga o caminho que voce achar melhor" — escolhi fechar o CI
(adiado duas vezes já) porque 73 testes sem nenhum gate automático era a
lacuna de maior risco nesse ponto. No caminho descobri que o projeto nunca
tinha sido inicializado como repositório git — todo o trabalho até aqui só
existia em disco, sem nenhum commit. Perguntei ao usuário como proceder;
escolheu inicializar git local agora, GitHub fica pra depois.

Feito: `git init` + primeiro commit (152 arquivos — conferi que nada de
`target/`, `.env` ou segredo entrou, só `.gitignore` já existente cobrindo
tudo). `.github/workflows/ci.yml`: job `changes` detecta path alterado
(`dorny/paths-filter@v4`) e dispara o job do serviço certo só se ele mudou;
cada job roda `mvn test` + `mvn dependency-check:check` (ADR-0017).
`dependency-check-maven` declarado nos dois `pom.xml` sem `<executions>`
(não roda no `mvn test` local, só explícito no CI). `.github/dependabot.yml`
pra Maven/Docker/docker-compose/github-actions.

Antes de fixar versões de actions/plugins, validei via busca web em vez de
chutar (mesmo princípio de sempre confirmar antes de assumir que usei a
sessão inteira): `dependency-check-maven` 13.0.0 (Maven Central,
2026-08-03), `actions/checkout@v7`, `actions/setup-java@v5` (v6 ainda em
desenvolvimento, não recomendado), `actions/cache@v6`, `dorny/paths-filter@v4`.

**Achado real rodando local**: `dependency-check-maven` falhou com "Invalid
API Key" — a NVD passou a exigir uma API key (gratuita) pra sincronizar a
base de CVE, sem isso o scan não roda de jeito nenhum, não é só mais lento.
Sem como testar isso de ponta a ponta sem uma chave de verdade (e sem
repositório no GitHub pra disparar o workflow ainda), documentei o passo
como pendência clara: configurar `NVD_API_KEY` nos secrets do GitHub antes
do primeiro PR real, senão todo PR falha o scan.

Criado/alterado: `.git/` (repo novo, commit inicial); `.github/workflows/ci.yml`;
`.github/dependabot.yml`; `account-service/pom.xml` e
`transaction-service/pom.xml` (plugin `dependency-check-maven`);
`docs/architecture/security.md` (inventário: `NVD_API_KEY`); `docs/tasks.md`.

## 2026-08-07 — Interfaces gráficas + guia completo do Postman

Pedido: documentar Keycloak e toda interface gráfica acessível via
Docker, tanto pra dev (ambiente atual) quanto pro padrão de produção
futura; criar um passo a passo completo pra testar o sistema inteiro no
Postman; e uma pergunta sobre como liberar acesso do Claude ao GitHub do
usuário.

Feito: `docs/architecture/interfaces-graficas.md` (novo) — tabela de dev
com todas as 8 interfaces (Keycloak, Kafka UI, Grafana, Prometheus,
Swagger/Dev UI dos dois serviços), validada subindo Prometheus/Grafana de
verdade (nunca tinham sido ligados nessa sessão). Seção de produção usa
placeholder de domínio (nunca dado real em arquivo versionado, regra do
`deployment.md`) e levanta uma decisão em aberto que não tomei sozinho:
Keycloak serve login público e console admin na mesma porta — não dá pra
tunelar só um. Registrei 3 opções com recomendação (Cloudflare Access no
path `/admin`), mas fica pro usuário decidir quando chegarmos na fatia 9.

`docs/postman/README.md` reescrito como checklist numerado (0 a 6): subir
infra → importar environment → importar as duas collections → token de
usuário → token de serviço → roteiro de 9 passos que exercita os dois
serviços de ponta a ponta (criar conta → transação → saldo mudou → cancela
→ saldo volta → exclui conta) → tabela de erros comuns (baseada nos bugs
reais que encontramos essa sessão: issuer do Keycloak, token expirado,
baseUrl da collection). Adicionei `transaction_service_url` que faltava no
environment.

Criado/alterado: `docs/architecture/interfaces-graficas.md` (novo);
`docs/postman/README.md` (reescrito); `docs/postman/financas-dev.postman_environment.json`;
`CLAUDE.md` (mapa de docs); `README.md` (link + Prometheus na tabela).

## 2026-08-07 — Conexão com o GitHub + CI validado de verdade

Pedido: fazer a parte de conexão com o GitHub do usuário.

`gh` CLI não estava instalado, e nem `winget`/`choco` disponíveis. O
instalador `.msi` oficial falhou com erro 1925 (precisa de admin, sem
elevação neste ambiente) — resolvido baixando a versão `.zip` portátil via
PowerShell, extraindo em `%LOCALAPPDATA%\Programs\gh` (sem precisar de
admin) e adicionando ao PATH do usuário. Login (`gh auth login`) é
interativo (código + navegador) — o usuário rodou num terminal próprio; na
primeira tentativa o terminal fechou antes de confirmar e o login não
persistiu (diretório de config nem existia), refeito com sucesso na
segunda tentativa.

Repositório criado: `github.com/wep1980/wepdev-financas`, privado
(decisão do usuário — projeto de finanças pessoais, sem motivo pra ficar
público por padrão). Branch local renomeada de `master` pra `main` antes
do primeiro push (nosso CI já esperava `main`, padrão atual do GitHub).

Push inicial só continha o commit já existente (sem o CI, que ainda
estava sem commitar de propósito) — commitei e subi separado o que faltava
(CI, Dependabot, docs de interfaces/Postman) pra poder testar de verdade.

**Bug real encontrado rodando no GitHub Actions** (não aparecia local):
primeiro run falhou com `./mvnw: Permission denied` (exit 126) — Windows
não rastreia bit de execução (`core.filemode=false` nessa máquina), então
o `mvnw` de cada serviço foi commitado como `100644` em vez de `100755`.
Corrigido com `git update-index --chmod=+x` nos dois `mvnw`, novo commit,
novo push. Depois disso: **`mvn test` passou nos dois serviços de
verdade no runner hospedado** (45 + 28 testes, Dev Services/Testcontainers
funcionando sem configuração extra). Só o scan de vulnerabilidade falhou,
exatamente como esperado — `Invalid API Key` por falta da `NVD_API_KEY`
(já documentado como pendência antes mesmo de existir repositório).

Efeito colateral notado: assim que `dependabot.yml` foi pro GitHub, o
Dependabot já abriu várias PRs de atualização automaticamente (esperado) —
essas PRs rodaram CI contra o commit anterior ao fix do `mvnw` e também
falharam por isso; devem se resolver sozinhas quando o Dependabot
rebasear, não é um problema novo.

Criado/alterado: `.git/` (branch renomeada `master`→`main`, remoto
`origin` configurado, 3 commits, push feito); `services/*/mvnw` (modo
100755); `docs/tasks.md`; `README.md` (link do repo real).

## 2026-08-07 — Segundo bug do CI (achado contra PR real do Dependabot) + limpeza

Pedido: "vamos resolver essa parte do github, tudo que você conseguir
fazer, faça — o que você não conseguir me diga passo a passo."

Fiz sozinho, sem precisar do usuário: pedi `@dependabot rebase` nas 9 PRs
que o Dependabot já tinha aberto sozinho (pra tirar o X vermelho causado
pelo bug do `mvnw` da sessão anterior); adicionei badge de status do CI no
`README.md`.

No processo de rebasear as PRs, achei um **segundo bug real de CI**, que só
aparece em PR (não em push direto pra `main`, por isso não tinha aparecido
antes): `dorny/paths-filter` falhava com "Resource not accessible by
integration" — PRs de fonte externa (Dependabot é tratado como tal) recebem
`GITHUB_TOKEN` com permissão de leitura mínima por padrão, sem acesso pra
listar arquivos alterados da PR via API. Corrigido com bloco `permissions`
explícito (`contents: read`, `pull-requests: read`) no workflow. Pedi
rebase de novo nas 9 PRs pra validar — as 4 de `docker-compose` (não tocam
`services/`) e as de Maven (que tocam, disparando os testes de verdade)
todas passaram até a etapa de teste; só o scan de vulnerabilidade falha,
exatamente como esperado, por falta da `NVD_API_KEY`.

O que não consegui fazer sozinho (expliquei ao usuário): gerar a
`NVD_API_KEY` em si — é um cadastro pessoal no site da NVD (nist.gov),
amarrado à identidade de quem cadastra, não é algo que eu deva fazer em
nome dele. Ofereci configurar o secret no GitHub via `gh secret set` assim
que ele tiver o valor.

Criado/alterado: `.github/workflows/ci.yml` (bloco `permissions`);
`README.md` (badge de CI); `docs/tasks.md`. 9 PRs do Dependabot comentadas
(`@dependabot rebase`, duas rodadas).
