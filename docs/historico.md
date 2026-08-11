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

## 2026-08-07 — Documentação de GUIs/URLs, guia de Postman e conexão com GitHub

Pedido (três partes numa mensagem só): documentar Keycloak e todas as
interfaces gráficas acessíveis via Docker (dev e produção); criar um
passo a passo de tudo que precisa ser feito pra testar o sistema inteiro
no Postman; explicar como liberar acesso da IA ao GitHub do usuário e o
que dá pra configurar com esse acesso.

Criado: `docs/architecture/interfaces-graficas.md` (Keycloak :8080,
Kafka UI :8090, Grafana :3001, Prometheus :9090, Dev UI/Swagger dos dois
serviços — dev e produção, com placeholder de domínio); reescrita de
`docs/postman/README.md` (guia 0-6: pré-requisitos → import → token de
usuário → token de serviço → roteiro fim-a-fim de 9 passos → erros
comuns). Expliquei que o acesso ao GitHub seria via `gh` CLI autenticado
localmente, não um token/app compartilhado.

Sessões seguintes (mesmo dia): usuário pediu login no GitHub passo a
passo; instalação do `gh` CLI **via PowerShell** (não download manual) —
instalado sem privilégio de admin usando o `.zip` portátil (não o
`.msi`, que pede elevação), extraído em
`%LOCALAPPDATA%\Programs\gh\bin`, PATH de usuário atualizado (só vale
pra processos novos, não pro processo da sessão já aberta — precisei
usar caminho completo ou `export PATH=...` no Bash daqui pra frente).
`gh auth login` é interativo, rodado pelo usuário num terminal separado;
primeira tentativa não persistiu (fechou o terminal antes da confirmação
"Logged in as..."), segunda tentativa funcionou. Repositório
`wepdev-financas` criado como privado no GitHub, remoto conectado, push
inicial feito.

Depois disso, pedido explícito: "tudo que você conseguir fazer sozinho
no GitHub, faça; o que não conseguir, explique passo a passo." Configurei
sozinho: branch `main` (renomeada de `master`), CI (`.github/workflows/ci.yml`,
ver entradas acima), `.github/dependabot.yml`, e o secret `NVD_API_KEY`
nos **dois** stores do GitHub que existem pra isso — `gh secret set
NVD_API_KEY --app actions` (Actions normais) e `--app dependabot`
(store separado, só descoberto porque as PRs do Dependabot continuavam
vendo a chave vazia mesmo com o primeiro configurado). O valor da chave em
si (cadastro pessoal, amarrado ao e-mail do usuário no site da NVD/NIST)
não é algo que eu deva gerar — o usuário colou o valor depois de se
cadastrar. Ficou pendente a ativação da chave (confirmação por e-mail):
o usuário reportou que o link de confirmação da NVD não abria; testei via
`curl`/`WebFetch` e confirmei 403/502 vindos do próprio site nvd.nist.gov
(instabilidade do lado deles, não erro de configuração nossa) —
combinamos deixar isso de lado e voltar pra implementação de
funcionalidades; CI segue verde exceto o scan de vulnerabilidade, que
falha só por causa dessa chave não ativada.

Criado/alterado: `docs/architecture/interfaces-graficas.md` (novo);
`docs/postman/README.md` (reescrito); `docs/architecture/security.md`
(linha da `NVD_API_KEY` no inventário de credenciais); `.github/dependabot.yml`;
`docs/tasks.md`; `README.md`.

## 2026-08-08 — Editar transação (`PUT /transacoes/{id}`)

Pedido: "certo, podemos voltar à implementação" (depois de deixar a
pendência da `NVD_API_KEY` de lado). Escolhi seguir com **editar
transação** como próximo passo natural do CRUD de `transaction-service`
(registrar → listar → cancelar → editar), como nas sessões anteriores em
que o usuário pediu pra eu escolher o próximo passo.

Implementado: `Transacao.atualizar(descricao, valor, categoria,
dataTransacao)` — `contaId`/`tipo`/`usuarioId` não são editáveis de
propósito (trocar de conta/tipo é cancelar e recriar, evita ambiguidade
de reversão de saldo entre contas diferentes); `TransacaoCanceladaException`
(nova, 422) pra impedir editar uma transação já cancelada;
`AtualizarTransacaoUseCase` — se o valor mudou, ajusta o saldo pela
**diferença líquida (delta)** numa chamada só ao `account-service`, em
vez de reverter-e-reaplicar em duas chamadas (evita uma janela de
inconsistência se a segunda chamada falhasse); endpoint `PUT
/api/v1/transacoes/{id}`. 9 testes unitários novos
(`AtualizarTransacaoUseCaseTest`) + 7 de integração (`TransacaoResourceTest`,
cobrindo delta pra cima/baixo, sem mudança de valor, 404 inexistente/de
outro usuário, 422 cancelada, 400 validação) — 47 testes no total do
serviço, todos passando; `account-service` sem regressão (45 testes).
Validado de ponta a ponta contra containers reais (rebuild + `docker
compose up`): conta com saldo 1000 → despesa de 100 → editar pra 150
(debita +50, saldo 850) → editar pra 60 (credita 90, saldo 940) →
cancelar → tentar editar de novo dá 422.

Criado/alterado: `docs/specs/transaction-service.yaml` (`required:
[descricao, valor]` em `AtualizarTransacaoRequest`, resposta 400
documentada, 422 ampliado pra mencionar transação cancelada);
`docs/architecture/diagrams.md` (seção 4.2 — `atualizar()` em `Transacao`,
nova classe `TransacaoCanceladaException`); `docs/postman/mudancas-manuais.txt`
(entrada nova pro PUT); `docs/tasks.md`.

Depois do commit/push, o usuário mandou a `NVD_API_KEY` que tinha acabado
de gerar (segunda tentativa, chave diferente da primeira que ficou presa
na confirmação por e-mail). Atualizei o secret nos dois stores do GitHub
(`gh secret set NVD_API_KEY --app actions` e `--app dependabot`) e
re-executei só o job que falha (`gh run rerun --failed`) pra testar sem
esperar um push novo.

## 2026-08-08 — Resumo por categoria (`GET /transacoes/resumo-por-categoria`)

Pedido: "vamos para o próximo passo" (escolhido por mim, seguindo o
backlog: com o CRUD de transação completo, o próximo item natural era o
endpoint de agregação já espec'ado, consumido depois pelo dashboard e pela
tool de IA).

Implementado: `ResumoPorCategoriaUseCase` — soma só `DESPESA` com status
`CONFIRMADA` num período (`RECEITA` e transação `CANCELADA` não contam);
transação sem categoria agrupa em "Sem categoria"; `percentualDoTotal`
calculado sobre o total gasto do próprio período; `totalGastoPeriodoAnterior`
compara com o período imediatamente anterior de mesma duração em dias
(nulo se a categoria não teve gasto nele); `IntervaloInvalidoException`
(nova, 400) se `início` vier depois do `fim`. `inicio`/`fim` viraram
`@NotNull` direto nos `@QueryParam` do endpoint (primeira vez nesse
serviço validando parâmetro de query, não só corpo — funcionou de graça
porque o `ConstraintViolationExceptionMapper` já existente também cobre
violação de parâmetro de método, não só `@Valid` no body).

Bug achado pelo próprio teste (não em produção): a ordenação por
`totalGasto` decrescente ficava não-determinística quando duas categorias
empatavam no total, porque o agrupamento usa `HashMap` internamente (sem
ordem garantida). Corrigido com desempate por nome da categoria
(`.thenComparing(categoria)`).

7 testes unitários (`ResumoPorCategoriaUseCaseTest`) + 5 de integração
(`TransacaoResourceTest`) — 59 testes no total do serviço, todos passando.
Validado de ponta a ponta contra containers reais: 150 de Alimentação + 50
de Transporte + 3000 de Receita (ignorada) num período → resumo retorna
Alimentação 75%, Transporte 25%; confirmei também que a rota literal
`/resumo-por-categoria` não conflita com `/{id}` (JAX-RS prioriza segmento
literal sobre variável de path automaticamente).

Criado/alterado: `docs/specs/transaction-service.yaml` (resposta 400
documentada no endpoint); `docs/tasks.md`.

## 2026-08-08 — CI verde de verdade: NVD_API_KEY ativada e 2 CVEs reais corrigidas

A `NVD_API_KEY` que o usuário gerou (segunda tentativa) funcionou —
primeira vez que o scan do OWASP Dependency-Check rodou até o fim contra
o CI. Isso revelou vulnerabilidades reais (não era mais só "chave
inválida"): `mysql-connector-j:9.7.0` com 5 CVEs (CVSS até 8.5) e
`opentelemetry-semconv`/`opentelemetry-semconv-incubating:1.41.1/1.40.0-alpha`
com CVEs de CVSS 7.3+ — ambas herdadas transitivamente do BOM do Quarkus
3.38.1, afetando os dois serviços igualmente (mesma árvore de dependência).

Perguntei ao usuário como preferia resolver (forçar versão nova via
`dependencyManagement`, atualizar a plataforma Quarkus inteira, ou
documentar como exceção temporária) — escolheu forçar a versão nova.
Sobrescrevi `mysql-connector-j` pra `26.7.0` (mudança grande de
versionamento da Oracle, não é só um patch — saltou de 9.x pra 26.x) e
`opentelemetry-semconv`/`-incubating` pra `1.43.0`/`1.43.0-alpha` (essas
são só constantes de convenção semântica, risco baixo). Overrides
declarados no `dependencyManagement` de cada `pom.xml`, **antes** do
import do `quarkus-bom` — necessário porque o Maven resolve por "primeira
ocorrência vence" na lista efetiva de `dependencyManagement`, então um
override depois do import seria ignorado.

Rodei a suíte de teste completa dos dois serviços (usa MySQL real via
Quarkus Dev Services, não mock) pra confirmar que o salto grande de versão
do connector não quebrou nada — 59 testes em `transaction-service` + 45 em
`account-service`, todos passando sem alteração de código, só a versão da
dependência mudou.

Criado/alterado: `services/account-service/pom.xml` e
`services/transaction-service/pom.xml` (overrides de versão);
`docs/tasks.md`; `docs/architecture/security.md` (`NVD_API_KEY` marcada
como ativa).

## 2026-08-08 — Transações recorrentes (fecha o backlog de `transaction-service`)

Pedido: "vamos para o próximo passo" — último item do backlog do
`transaction-service` (`docs/tasks.md`), conforme ADR-0009 (regra
recorrente é conceito genérico do `transaction-service`, distinto de
parcelamento de cartão do futuro `card-service`) e a spec já existente em
`docs/specs/transaction-service.yaml`.

Implementado: entidade `TransacaoRecorrente` (domínio) — frequência só
`MENSAL` no v1, `proximaDataVencimento()` calcula `dataInicio.plusMonths(ocorrenciasGeradas)`,
`registrarOcorrenciaGerada()` conclui automaticamente ao atingir
`quantidadeOcorrencias` (null = indefinida, nunca conclui sozinha).
`CriarTransacaoRecorrenteUseCase` **reusa `RegistrarTransacaoUseCase`**
pra gerar cada ocorrência — mesmo caminho síncrono com o
`account-service` de uma transação avulsa, sem duplicar lógica de saldo
(precisou de um pequeno ajuste: `Transacao.criar()` ganhou um overload
aceitando `transacaoRecorrenteId`, e `RegistrarTransacaoCommand` ganhou o
mesmo campo opcional). Job agendado (`GerarOcorrenciasRecorrentesJob`,
`quarkus-scheduler`, cron diário) é um wrapper fino sobre
`GerarOcorrenciasRecorrentesUseCase.executar(LocalDate hoje)` — a data
"hoje" é parâmetro explícito em vez de `LocalDate.now()` lido dentro da
lógica, o que permitiu testar geração de ocorrência, limite de
`quantidadeOcorrencias` e regra indefinida (24 execuções seguidas sem
concluir sozinha) sem `Thread.sleep` nem tempo real — desabilitado em
teste (`%test.quarkus.scheduler.enabled=false`) pra não rodar em paralelo
com a suíte. Endpoints REST completos (`POST`/`GET`/`GET {id}`/`DELETE`
+ `GET /proximos-vencimentos`, este último role `service`, consumido no
futuro pelo `notification-service` conforme ADR-0010).

Bug achado pelo próprio teste (não em produção, mesma classe de erro que
já apareceu no resumo por categoria): um teste de isolamento
(`naoDeveriaListarRegraDeOutroUsuario`) reusou o mesmo `sub` de outros
testes da classe que já tinham criado regras pra aquele usuário — como
não há rollback entre métodos de teste na mesma classe, a asserção
"size() == 0" falhou com 4 regras. Corrigido com um `sub` exclusivo pro
teste de isolamento (mesmo padrão já estabelecido com `SUB_LISTAGEM_*`
em `TransacaoResourceTest`).

19 testes unitários novos (7 domínio + 12 casos de uso, cobrindo os três
critérios do backlog: limite respeitado, indefinida nunca conclui
sozinha, cancelamento não gera mais ocorrências) + 12 de integração REST
— 91 testes no total do serviço, todos passando. Validado de ponta a
ponta contra containers reais: criar regra indefinida de salário →
saldo reflete a 1ª ocorrência imediatamente; criar regra com
`quantidadeOcorrencias=1` → nasce `CONCLUIDA`; listar filtrado por
`status=ATIVA`; cancelar → some da listagem, idempotente; chamar
`/proximos-vencimentos` com token de usuário dá 403, com token de
serviço (client_credentials do próprio `transaction-service`) retorna a
janela de dias correta (testado com uma regra cujo próximo vencimento
ficava fora de uma janela de 30 dias mas dentro de uma janela de 60).

Não implementado nesta sessão (registrado como pendência explícita):
teste de integração do **wrapper do scheduler** disparando via cron de
verdade — exigiria manipular relógio do container ou esperar tempo real;
o núcleo testável (`GerarOcorrenciasRecorrentesUseCase`) já está coberto,
o wrapper é só 5 linhas ligando `LocalDate.now()` a ele.

Isso fecha o backlog completo do `transaction-service` (fatia 1). Próxima
fatia do roadmap é `card-service`.

Criado/alterado (só `transaction-service`): domínio
(`TransacaoRecorrente`, `FrequenciaRecorrencia`,
`StatusTransacaoRecorrente`, `TransacaoRecorrenteNaoEncontradaException`,
`TransacaoRecorrenteRepository`); persistência (migração Flyway V2,
`TransacaoRecorrenteEntity`/`Mapper`/`PanacheRepository`/`RepositoryImpl`);
aplicação (`CriarTransacaoRecorrenteUseCase`, `ListarTransacoesRecorrentesUseCase`,
`BuscarTransacaoRecorrenteUseCase`, `CancelarTransacaoRecorrenteUseCase`,
`GerarOcorrenciasRecorrentesUseCase`, `ProximosVencimentosUseCase`); REST
(`TransacaoRecorrenteResource`, DTOs, exception mapper); scheduler
(`GerarOcorrenciasRecorrentesJob`); `pom.xml` (`quarkus-scheduler`);
`docs/specs/transaction-service.yaml` (respostas 400/404/422
documentadas no `POST`); `docs/architecture/diagrams.md` (seção 4.3
nova); `docs/tasks.md`.

## 2026-08-08 — Cobertura de teste (JaCoCo) e build de imagem Docker no CI

Pedido: usuário perguntou se dava pra usar Sonar no código e como
implementar no GitHub Actions, e se eu sugeria mais alguma coisa pro CI.
Expliquei duas opções — SonarCloud (hospedado, plano free cobre repo
privado até ~50k linhas, mas exige cadastro pessoal do usuário + um
`SONAR_TOKEN` novo, mesma dança da `NVD_API_KEY`) ou SonarQube self-hosted
(grátis mas mais um serviço pra manter no servidor único, só faz sentido
depois que a fatia 9/deploy existir) — usuário decidiu não configurar
Sonar por agora, só queria entender a opção. Sugeri duas coisas menores e
o usuário topou as duas: cobertura de teste (JaCoCo) e build de imagem
Docker como validação no CI.

Implementado: `jacoco-maven-plugin` (0.8.15) nos dois `pom.xml` —
`prepare-agent` injeta o javaagent via a property `@{argLine}` que o
surefire já esperava (só o `transaction-service` tinha isso configurado;
precisei adicionar `<argLine>@{argLine}</argLine>` no `account-service`
também); `report` bindado na fase `test`, roda dentro do `mvn test`
normal sem precisar de fase separada. Validado localmente: gera HTML com
contagem real de classes analisadas (63 em `transaction-service`, 30 em
`account-service`) — confirma que o `report` roda DEPOIS dos testes
mesmo os dois estando bindados na mesma fase `test` (ordem: bindings
automáticos do packaging, como o `surefire:test`, executam antes das
`<executions>` declaradas explicitamente no POM pra essa mesma fase).
CI publica o relatório como artefato do workflow run
(`actions/upload-artifact`, 14 dias de retenção) — sem publicar em
nenhum serviço externo.

Também adicionado: passo de build de imagem Docker (`mvn package
-DskipTests` + `docker build -f src/main/docker/Dockerfile.jvm`) em cada
job do CI, só pra validar que a imagem continua buildando a cada mudança
— não publica em registry nem faz deploy (isso continua reservado pra
fatia 9, ADR-0020/0021). Runners `ubuntu-latest` já vêm com Docker
instalado. Validado localmente antes do push (mesmo padrão da sessão:
nunca confiar em config sem rodar de verdade).

Criado/alterado: `services/account-service/pom.xml` e
`services/transaction-service/pom.xml` (plugin JaCoCo + `argLine` no
account-service); `.github/workflows/ci.yml` (passos de cobertura e
build Docker nos dois jobs); `docs/tasks.md`.

## 2026-08-08 — Comentário automático de cobertura na PR

Pedido: usuário perguntou se existe interface gráfica pro JaCoCo.
Expliquei que o próprio relatório HTML (já sendo gerado e publicado como
artefato do CI) É a interface gráfica — árvore de pacotes/classes, %
de cobertura, código-fonte com linha destacada por cobertura — só
precisa baixar o artefato do run e abrir `index.html` local. Perguntei se
o usuário queria alguma forma de ver isso sem baixar manualmente a cada
run; ele topou um comentário automático de cobertura na Pull Request.

Implementado: `madrapps/jacoco-report@v1.8.0` (confirmado via GitHub
Releases da action) nos dois jobs do CI, lendo o `jacoco.xml` que o
JaCoCo já gera junto com o HTML. Só roda em evento `pull_request`
(`if: github.event_name == 'pull_request'`) — não faz sentido em push
direto pra `main`, não existe PR pra comentar. Precisou adicionar
`permissions: pull-requests: write` a nível de job nos dois jobs de
serviço, sobrescrevendo o `pull-requests: read` do topo do arquivo (que
segue valendo pro job `changes` e como default) — mesmo padrão já usado
antes pro `dorny/paths-filter` funcionar em PR do Dependabot.

Não validado contra uma PR real nesta sessão (não havia PR aberta) — só
localmente, confiando no comportamento padrão e bem documentado do `if:`
condicional do GitHub Actions. Validar na próxima PR real que rodar CI.

**Atualização, mesmo dia**: usuário perguntou se existe interface gráfica
pro JaCoCo — expliquei que o próprio relatório HTML já gerado É a
interface (árvore de pacotes/classes, % de cobertura, código-fonte com
linha destacada por cobertura) e que basta baixar o artefato do run.
Perguntei se ele queria ver isso sem baixar manualmente toda vez; topou o
comentário automático na PR. Abri uma PR de teste real (branch
`ci/test-jacoco-pr-comment`, mudança trivial no README do
`transaction-service` — adicionar "scheduling" na lista de pacotes,
PR #10) só pra validar o passo. Confirmado: comentário postado
corretamente (`Cobertura de teste — transaction-service — Overall
Project: 80.06%`). Fechei a PR sem merge e limpei a branch (local +
remota) depois de confirmar.

Criado/alterado: `.github/workflows/ci.yml` (step novo + permissions
por job nos dois jobs de serviço); `docs/tasks.md`.

## 2026-08-08 — Início da fatia 2: spec do `card-service`

Pedido: "vamos pro próximo passo do roadmap (card-service)".

Antes de escrever a spec, achei uma decisão de arquitetura real não
resolvida: `account-service` já tem `TipoConta.CARTAO_CREDITO` desde a
fatia 1, mas `overview.md` já descrevia `card-service` como dono de
"cartões, faturas, parcelamento" com banco próprio (`card_db`). Perguntei
ao usuário como os dois deveriam se relacionar — três opções (independente
sem depender do tipo / cartão sempre amarrado a uma Conta CARTAO_CREDITO /
remover CARTAO_CREDITO agora). Escolheu independente. Documentei a decisão
em **ADR-0022**: `Cartao` do `card-service` nunca é uma `Conta`; tem um
`contaPagamentoId` apontando pra uma `Conta` `CORRENTE`/`POUPANCA`/
`CARTEIRA` (nunca `CARTAO_CREDITO`, evita circularidade) que é debitada de
forma síncrona ao pagar a fatura — mesmo padrão já validado de
`transaction-service` → `account-service`. `TipoConta.CARTAO_CREDITO`
continua existindo, sem uso rico associado (fica pra quem quer só anotar
manualmente, sem configurar um cartão de verdade).

Escrevi `docs/specs/card-service.yaml` (spec-driven, antes de qualquer
código, CLAUDE.md princípio 1): `Cartao` (apelido, bandeira opcional,
limite, diaFechamento, diaVencimento, contaPagamentoId), `Fatura`
(competência AAAA-MM, dataFechamento/Vencimento, valorTotal, status
ABERTA/FECHADA/PAGA — sem endpoint de criação manual, gerada
automaticamente ao lançar a 1ª compra da competência), Compra/Parcela
(uma compra com `quantidadeParcelas` gera parcelas em faturas
consecutivas). Endpoints: CRUD de cartão, lançar compra
(`POST /cartoes/{id}/compras`), listar/buscar fatura, pagar fatura
(`POST /faturas/{id}/pagar`, síncrono com account-service, idempotente),
e `GET /faturas/proximos-vencimentos` (role `service`, mesmo padrão do
endpoint análogo em `transaction-service`, pro futuro
`notification-service`).

`docs/tasks.md` reescrito: fatia 1 comprimida num resumo curto (detalhe
completo já vive em `docs/historico.md` e no git — não duplicado), fatia 2
detalhada com a ordem sugerida de implementação (vertical, um item de cada
vez, mesmo padrão usado em `transaction-service`). `docs/roadmap.md`,
`docs/architecture/overview.md` e `docs/architecture/diagrams.md`
(seção 3.3 nova — modelo ER conceitual do `card-service`, mesmo padrão já
usado pro `notification-service` antes de ter código) atualizados pra
refletir fatia 1 concluída e fatia 2 com spec pronta.

Criado/alterado: `docs/architecture/adr/0022-card-service-independente-de-conta.md`
(novo); `docs/specs/card-service.yaml` (novo); `docs/tasks.md` (reescrito);
`docs/roadmap.md`; `docs/architecture/overview.md`;
`docs/architecture/diagrams.md`.

## 2026-08-09 — `card-service`: scaffold + CRUD de cartão

Continuação direta da sessão anterior (mesmo pedido "vamos pro próximo
passo do roadmap") — depois da spec/ADR prontas, implementei a primeira
fatia vertical do `card-service`, mesmo padrão usado em
`transaction-service` (scaffold → domínio → casos de uso → REST → testes
→ validação real → documentação, um item de cada vez).

Scaffold: projeto Quarkus novo copiando a estrutura do
`transaction-service` (mvnw, Dockerfiles, `.mvn/wrapper`, plugins JaCoCo/
dependency-check já configurados desde o início, aprendendo com o que já
foi corrigido nos outros dois serviços). Banco `card_db` — precisou criar
manualmente no container MySQL já rodando (`docker exec ... CREATE
DATABASE`), porque o script `infra/mysql/init/01-databases.sql` só roda
na primeira subida do volume (mesmo motivo que já tinha exigido tratamento
manual no Keycloak antes). Client `card-service` no Keycloak também
criado direto no realm já rodando via `kcadm.sh` (`docker exec` com
`MSYS_NO_PATHCONV=1` — sem isso o git-bash do Windows reescreve o path do
binário dentro do container e quebra o `docker exec`), com o
service-account recebendo a role `service` — confirmado gerando um token
client_credentials de verdade e decodificando o JWT.

Domínio: `Cartao` (apelido, bandeira opcional, limite, diaFechamento/
diaVencimento, contaPagamentoId, ativo) + `AccountServiceClient`
(`confirmarPosseDaConta`) — copiei quase literalmente o client HTTP do
`transaction-service` (`AccountServiceUsuarioClient` +
`PropagarAutorizacaoHeadersFactory`), já que a lógica "confirma posse via
GET /contas/{id} com o token do próprio usuário repassado" é idêntica.
Casos de uso criar/listar/buscar/atualizar/excluir, todos seguindo o
padrão anti-IDOR já estabelecido (404 pra id inexistente OU de outro
usuário). `AtualizarCartaoUseCase` reconfirma a posse de
`contaPagamentoId` a cada atualização, já que o campo pode mudar pra uma
conta que não é mais do usuário.

Bug de teste achado e corrigido (mesma classe de erro que já apareceu
duas vezes antes nesta sessão, com `resumo por categoria` e `transações
recorrentes`): teste de isolamento reusando um `sub` que outros métodos da
mesma classe já tinham usado pra criar cartão, quebrando a asserção
"size() == 0" — corrigido com um `sub` exclusivo (`SUB_ISOLAMENTO`), mesmo
padrão já formalizado nas outras duas ocorrências.

7 testes de domínio + testes de caso de uso pra cada operação + 12 de
integração REST — 34 testes no total do serviço, todos passando. Validado
de ponta a ponta contra containers reais: criar conta de pagamento →
criar cartão → listar → buscar → atualizar (trocando `contaPagamentoId`)
→ excluir (idempotente, cartão inativo some da listagem mas continua
buscável por id) → criar cartão com `contaPagamentoId` inexistente dá
404. CI ganhou o job `card-service`, mesmo template dos outros dois
(testes + cobertura JaCoCo + build de imagem Docker de validação +
dependency-check) — já nasce com tudo isso configurado, sem precisar
descobrir de novo o que já foi aprendido nos outros serviços.

Criado: `services/card-service/` inteiro (scaffold + `Cartao` + CRUD +
testes + README). Alterado: `docker-compose.yml` (serviço `card-service`);
`.env.example` (`CARD_SERVICE_CLIENT_SECRET`); `infra/keycloak/realm-financas.json`
(client + service-account `card-service`); `infra/mysql/init/01-databases.sql`
(`card_db`); `.github/workflows/ci.yml` (job `card-service`);
`docs/architecture/diagrams.md` (seção 4.4 — diagrama de classe de
`Cartao`); `docs/tasks.md`; `README.md` (raiz — estado atual, endpoints,
URLs); `docs/postman/README.md` e `docs/postman/mudancas-manuais.txt`
(card-service, primeira vez); `docs/postman/financas-dev.postman_environment.json`
(`card_service_url`). Artifact de diagramas de classe republicado com a
seção `Cartao`.

## 2026-08-09 — Entre sessões: usuário está aprendendo com o projeto

Antes de continuar o `card-service`, o usuário contou que está usando o
`wepdev-financas` deliberadamente pra **aprender** as tecnologias
envolvidas (Quarkus, Kafka, Keycloak/OIDC, etc.) — não domina boa parte
delas ainda — e que a intenção de longo prazo, depois do sistema completo
(back-end + web + mobile), é criar um **framework de criação de
sistemas** baseado no aprendizado daqui. Perguntei o quanto de explicação
ele queria daqui pra frente ao introduzir algo novo; escolheu "breve por
padrão, aprofundo se ele perguntar". Registrado em memória
(`user_aprendizado_tecnologias.md` e `project_framework_pos_sistema_completo.md`)
pra sessões futuras — não é uma tarefa deste projeto, é contexto de como
colaborar.

## 2026-08-09 — `card-service`: fatura e parcelamento (fecha a fatia 2)

Pedido: "vamos continuar com fatura e parcelamento do card-service".
Segui a ordem já planejada em `docs/tasks.md`: domínio → persistência →
lançar compra/listar/buscar fatura → testes → pagar fatura → job de
fechamento + próximos vencimentos → validação real → documentação — um
item de cada vez, mesmo padrão de `transaction-service`.

Domínio: `Fatura` (nasce `ABERTA` com `valorTotal=0`, `adicionarParcela()`
acumula, `fechar()`/`pagar()` idempotentes) e `Parcela` (imutável, sem
classe "Compra" persistida — uma compra é só o agrupamento lógico de
`Parcela`s com o mesmo `compraId`). `LancarCompraUseCase`: decide a
competência da 1ª parcela comparando o dia da compra com o dia de
fechamento do cartão (antes → mês corrente, senão → mês seguinte),
cria cada `Fatura` sob demanda (uma por `cartaoId`+competência, sem
endpoint de criação manual), divide `valorTotal` pelas parcelas
(`HALF_UP`, 2 casas) absorvendo a sobra do arredondamento na última —
validado com teste e com curl real: 100.00 em 3x virou
33.33+33.33+33.34, soma batendo exatamente.

`PagarFaturaUseCase`: só paga fatura `FECHADA` (422 se `ABERTA`), debita
a `contaPagamentoId` do cartão **antes** de marcar `PAGA` (se falhar, a
fatura continua `FECHADA`, sem "pagamento fantasma"), idempotente.
Precisou adicionar `debitar()` na porta `AccountServiceClient` do
`card-service` — copiei o `AccountServiceInternoClient` (token de
serviço via `quarkus-rest-client-oidc-filter`) quase igual ao do
`transaction-service`.

`FecharFaturasVencidasJob` (`quarkus-scheduler`, cron diário) fecha
fatura `ABERTA` vencida automaticamente — mesmo padrão de núcleo
testável + wrapper fino já usado no `GerarOcorrenciasRecorrentesJob` do
`transaction-service` (recebe "hoje" como parâmetro). `GET
/faturas/proximos-vencimentos` (role `service`) só considera `FECHADA`
(nem `ABERTA`, que não tem valor definitivo, nem `PAGA`, que já não é
mais pendente).

82 testes no total do serviço (16 novos de domínio, 26 novos de caso de
uso, 16 novos de integração REST), todos passando de primeira — nenhum
bug de isolamento entre testes dessa vez (lição das duas ocorrências
anteriores já virou hábito: sub exclusivo sempre que o teste faz
asserção de tamanho de lista). Validado de ponta a ponta contra
containers reais, incluindo o caminho que os testes mockam: paguei uma
fatura de verdade e confirmei o saldo da conta de pagamento no
`account-service` caindo exatamente o valor esperado (5000.00 →
4866.67), idempotência confirmada numa segunda chamada, e o endpoint de
próximos vencimentos retornando o `apelidoCartao` corretamente pro
service-account real. Como não existe endpoint pra fechar fatura
manualmente (fica só pro job), fechei uma fatura direto no banco via SQL
pra poder testar o pagamento sem esperar o cron — documentado no
changelog do Postman como o jeito de testar isso manualmente.

Isso fecha a fatia 2 do roadmap (`card-service`) por completo. Próxima
fatia é `document-service` (fatia 3) — ainda sem contrato OpenAPI.

Criado: domínio (`Fatura`, `Parcela`, `StatusFatura`,
`FaturaNaoEncontradaException`, `FaturaAindaAbertaException`,
`SaldoInsuficienteException`, `FaturaRepository`, `ParcelaRepository`);
persistência (migração V2, entities/mappers/repositories);
aplicação (`LancarCompraUseCase`, `ListarFaturasUseCase`,
`BuscarFaturaUseCase`, `PagarFaturaUseCase`,
`FecharFaturasVencidasUseCase`, `ProximosVencimentosUseCase`); REST
(`FaturaResource` novo, `CartaoResource` ganhou `/compras` e `/faturas`,
DTOs, exception mappers); scheduler (`FecharFaturasVencidasJob`);
`AccountServiceInternoClient` + `debitar()` na porta. Alterado:
`docs/specs/card-service.yaml` não precisou mudar (implementação seguiu
o contrato já escrito à risca); `docs/architecture/diagrams.md` (seção
4.4 revisada + 4.5 nova, seção 3.3 atualizada); `docs/architecture/overview.md`;
`docs/roadmap.md`; `docs/tasks.md` (reescrito, fatia 2 comprimida como
concluída, fatia 3 como próxima); `README.md` (raiz e do serviço);
`docs/postman/mudancas-manuais.txt`; artifact de diagramas de classe
republicado.

## 2026-08-09 — `document-service`: kickoff da fatia 3 (infra e spec)

Pedido: "vamos para o proximo passo" → início da fatia 3 (`document-service`).
Três decisões em aberto foram levantadas e confirmadas pelo usuário antes de
escrever a spec: (1) estratégia de ingestão por foto — ADR-0015 mudou de
Proposta pra Aceita, confirmando visão do LLM direto na imagem, sem OCR
separado; (2) provedor de LLM pra dev/teste — Ollama local, não OpenAI (dado
financeiro sensível não sai da máquina, sem custo por chamada); (3) escopo
da primeira fatia vertical — só fatura de cartão em PDF (extrato, boleto e
foto ficam pra depois).

Consequência de infra: subido serviço `ollama` novo no `docker-compose.yml`
(porta 11434, volume próprio) e baixado o modelo `llama3.1` (~4.9GB) via
`ollama pull` dentro do container — validado com uma chamada real de
chat-completion antes de seguir. Ainda não commitado.

Escrita a spec `docs/specs/document-service.yaml` (upload multipart de PDF,
listar documentos, buscar detalhe com lançamentos, confirmar lançamentos
selecionados — publica evento Kafka `documento.lancamentos-confirmados`
seguindo o fluxo já desenhado em `overview.md` seção 3). Durante o desenho
da spec surgiu uma ambiguidade não coberta por nenhum documento existente:
uma fatura importada poderia, em teoria, alimentar o modelo de
`Fatura`/`Parcela` do `card-service` (fatia 2) em vez de virar `Transacao`
avulsa. Registrado como [ADR-0023](architecture/adr/0023-document-service-primeira-fatia-escopo.md):
decidido **não integrar** com `card-service` nessa fatia (evita acoplamento
novo e especulação sobre um requisito não pedido) e usar **Apache PDFBox**
pra extração de texto do PDF (mais adotada no ecossistema Java, licença
compatível).

`docs/tasks.md` reescrito com o backlog detalhado da fatia 3 (11 passos,
mesmo nível de detalhe dado à fatia 2 originalmente) — nenhum código de
`document-service` foi escrito ainda, próximo passo é o scaffold do
serviço.

Criado: `docs/specs/document-service.yaml`,
`docs/architecture/adr/0023-document-service-primeira-fatia-escopo.md`.
Alterado: `docs/architecture/adr/0015-ingestao-foto-visao-llm.md` (status →
Aceita), `docker-compose.yml` (serviço `ollama`), `docs/tasks.md` (fatia 3
detalhada).

## 2026-08-09 — `document-service`: itens 2–5 do backlog (domínio até validação real)

Pedido: continuar a fatia 3 item a item ("sim" repetido) até chegar no
pedido explícito de testar com uma fatura real que o usuário tinha em mãos
— fatura Santander, no nome da titular (mãe/parente), com o uso do usuário
aparecendo como dependente numa seção própria, e protegida por senha (CPF
do titular). Usuário perguntou se dava pra abrir a fatura sem digitar CPF
toda vez — resposta foi que dá pra automatizar (PDFBox aceita senha via
código), só não dá pra abrir um PDF criptografado sem NENHUMA senha válida.

Criada pasta `test-data/` (raiz do projeto, gitignored exceto o README) pra
guardar esse tipo de arquivo de referência com dado real de terceiro — não
podia ir pro histórico do git mesmo em repo privado.

Implementado nessa sessão: domínio completo (`DocumentoImportado`,
`LancamentoPendente`, persistência dividida Mongo+MySQL), porta
`LlmProvider` + `OllamaLlmProvider`, extração de PDF via PDFBox +
`AgenteExtracaoFaturaService` (agente de parsing). Documentado em detalhe
nos itens 2–4 de `docs/tasks.md`.

O item 5 (validação com a fatura real) só passou depois de resolver três
problemas de verdade, nenhum previsto no design original:
1. PDF protegido por senha — `ExtratorTexto` ganhou parâmetro `senha` +
   `PdfProtegidoPorSenhaException`.
2. Existe um Ollama **nativo instalado no Windows**, fora do Docker,
   escutando especificamente `127.0.0.1:11434` — roubava toda conexão
   IPv4 pra "localhost:11434" mesmo com o container Docker publicando a
   mesma porta (bind mais específico do Windows vence o wildcard do Docker
   Desktop), causando um 404 "model not found" bem enganoso (o Ollama
   respondendo não era o container, que tinha o modelo baixado). Resolvido
   remapeando a porta do container pra 11500 no host.
3. A fatura real tem titular + dependente em seções separadas, e um prompt
   de ~14KB pedindo pro LLM (`llama3.1` local, 8B) achar a seção certa E
   montar o JSON no formato pedido não funcionou de forma confiável — o
   modelo às vezes inventava outro esquema JSON inteiro. Resolvido movendo
   esse trabalho pro código Java: `AgenteExtracaoFaturaService` agora
   recorta a seção da pessoa certa por regex no cabeçalho de cada cartão
   (exigindo primeiro E último nome do filtro baterem, pra não confundir
   com sobrenome de família compartilhado) e detecta o ano da fatura pela
   linha "Vencimento", ANTES de montar o prompt — o LLM só recebe ~5KB já
   filtrado e só precisa extrair lançamentos, não mais filtrar nem inferir
   ano. Resultado: 38 lançamentos extraídos corretamente restritos à seção
   do dependente, conferidos manualmente contra o PDF (datas/valores
   batendo). Duas imperfeições aceitas como esperadas do modelo local (não
   bloqueantes, dado que o fluxo exige confirmação do usuário antes de
   qualquer coisa virar transação — PRD 3.2): 2 de 10 parcelas idênticas
   de um mesmo comerciante descartadas, e 3 estornos vieram como `DESPESA`
   em vez de `RECEITA`.

Achado anotado pra quando o item 6 (endpoint de upload) for implementado,
não resolvido ainda: inferência local em CPU levou vários **minutos** (não
segundos) pra gerar ~40 lançamentos, mesmo já com o prompt reduzido — a
spec atual descreve o upload como síncrono (resposta 201 já com os
lançamentos), o que pode não aguentar fatura grande numa única requisição
HTTP. O domínio já modela um estado `PROCESSANDO` intermediário, então dá
pra virar assíncrono/polling sem redesenhar nada, só mexer na spec/REST.

Criado (`services/document-service/`): domínio completo, persistência
(Mongo + MySQL + migração Flyway), `LlmProvider`/`OllamaLlmProvider`,
`ExtratorTexto`/`PdfBoxExtratorTexto`, `AgenteExtracaoFaturaService`, 62
testes automatizados + 1 teste manual/exploratório
(`ExtracaoFaturaRealManualTest`, roda só localmente contra `test-data/` e
Ollama real, se auto-pula em CI). Criado `test-data/README.md`. Alterado:
`docker-compose.yml` (porta do `ollama` 11434→11500 no host),
`application.properties` do `document-service`, `.gitignore` (regra pra
`test-data/`), `docs/tasks.md` (itens 2–5 detalhados).

## 2026-08-09 — `document-service`: itens 6–7 (upload assíncrono + GET)

Pedido: "vamos seguir a sua sugestão" (tornar o upload assíncrono, dado o
achado do item anterior de que a inferência local leva minutos) + usuário
colocou mais faturas reais em `test-data/` (Itaú PDF sem senha, Nubank PDF
fechada, Nubank CSV — CSV é fatura ainda aberta, PDF é fatura fechada;
CSV fica fora de escopo por enquanto, ADR-0023 já limitou essa fatia a
PDF).

Registrado [ADR-0024](architecture/adr/0024-upload-documento-assincrono.md):
`POST /documentos` deixa de ser síncrono. Spec (`document-service.yaml`)
atualizada: resposta 202 imediata (status `RECEBIDO`, sem lançamento),
cliente sonda `GET /documentos/{id}` até sair de `PROCESSANDO`; ganhou
também os campos `senha` e `nomeFiltro` no multipart (achados reais da
sessão anterior, agora expostos na API de verdade).

Implementado: `UploadDocumentoUseCase` (persiste RECEBIDO, só depois
despacha `ProcessarDocumentoService` via `ManagedExecutor` — nessa ordem,
nunca a inversa, senão o job assíncrono pode rodar antes da transação
commitar), `ProcessarDocumentoService` (RECEBIDO→PROCESSANDO→
AGUARDANDO_CONFIRMACAO/ERRO_PROCESSAMENTO), `ListarDocumentosUseCase`,
`BuscarDocumentoUseCase`, `DocumentoResource` (upload multipart, listagem,
detalhe).

Dois achados técnicos reais no caminho: (1) leitura JPA via Hibernate ORM
Panache também precisa de transação/contexto ativo quando chamada de uma
thread do `ManagedExecutor` (fora do request HTTP) — `buscarPorId()` do
repositório teve que ganhar `@Transactional` também, não só `salvar()`;
(2) MongoDB com `@BsonId` do tipo `UUID` exige
`quarkus.mongodb.uuid-representation=standard` explícito no
`application.properties`, senão `CodecConfigurationException` ao gravar
(só apareceu ao rodar teste de integração de verdade, não no `compile`).

Testes: 24 novos — 5 unitários (`UploadDocumentoUseCaseTest`,
`ProcessarDocumentoServiceTest`, `ListarDocumentosUseCaseTest`,
`BuscarDocumentoUseCaseTest`) + `DocumentoResourceTest` com 9 cenários de
integração (`@QuarkusTest` + Testcontainers Mongo/MySQL, `LlmProvider`
mockado via `QuarkusMock.installMockForType` — mesmo padrão do
`AccountServiceClientImpl` no card-service — e `Awaitility` pra esperar o
processamento assíncrono terminar sem sleep arbitrário, dependência de
teste nova, primeiro serviço com fluxo assíncrono). 70 testes no total do
`document-service` agora, suite inteira verde.

Criado: `docs/architecture/adr/0024-upload-documento-assincrono.md`,
`UploadDocumentoCommand`, `UploadDocumentoUseCase`,
`ProcessarDocumentoService`, `ListarDocumentosUseCase`,
`BuscarDocumentoUseCase`, `DocumentoResource` + DTOs
(`DocumentoImportadoResponse`, `LancamentoPendenteResponse`,
`UploadDocumentoForm`, `ErroResponse`), `DocumentoNaoEncontradoExceptionMapper`.
Alterado: `docs/specs/document-service.yaml` (upload assíncrono, campos
`senha`/`nomeFiltro`), `DocumentoRepositoryImpl` (`@Transactional` em
`salvar()` e `buscarPorId()`), `application.properties`
(`quarkus.mongodb.uuid-representation`), `pom.xml` (Awaitility, teste),
`docs/tasks.md` (itens 6–7 detalhados).

## 2026-08-09 — `document-service`: validação com Itaú/Nubank + item 8 (confirmação + Kafka)

Pedido: testar a extração com as faturas reais adicionais que o usuário
colocou em `test-data/` (Itaú PDF sem senha, Nubank PDF fechado — os CSVs
do Nubank são fatura ainda aberta, fora de escopo por ora) e, em seguida,
implementar o item 8 do backlog (endpoint de confirmação).

Fatura Itaú: resultado excelente, 10 lançamentos, soma batendo 100% com o
total do PDF — o LLM até excluiu sozinho o "Pagamento efetuado" e a
parcela de próxima fatura, sem instrução explícita. Fatura Nubank:
primeira tentativa deu 0 lançamentos (todos descartados silenciosamente,
best-effort) — a causa era um bug real, não limitação do modelo: o Nubank
escreve data como "10 JUN" (dia + mês abreviado em português) e vencimento
como "17 JUL 2026", formato bem diferente do "DD/MM"/"DD/MM/AAAA" do
Santander/Itaú que o parser só sabia ler. Corrigido com um segundo
formato de parsing em `LancamentoExtraidoDto.parsearData` (mapeamento
manual JAN–DEZ) e `AgenteExtracaoFaturaService.detectarAnoReferencia`.
Depois do fix: 11 lançamentos, soma batendo com o subtotal do cartão.

Implementado o item 8: `POST /api/v1/documentos/{id}/confirmar`. Ao
desenhar isso apareceu um gap real na spec — pra criar uma `Transacao` de
verdade, o `transaction-service` precisa saber em qual conta debitar, e o
document-service nunca teve essa informação (ADR-0023, sem integração com
card-service). Resolvido adicionando `contaId` obrigatório em
`ConfirmarLancamentosRequest` — decidido no momento da confirmação, não do
upload. `DocumentoEventPublisher` publica UM evento
`documento.lancamentos-confirmados` (Kafka) com a lista de lançamentos
confirmados + `contaId`; idempotente (documento já `CONFIRMADO` não
republica). Dois `ExceptionMapper` novos (422): `NenhumLancamentoSelecionadoException`
(lista vazia) e `DocumentoAindaNaoProcessadoException` (documento fora do
status `AGUARDANDO_CONFIRMACAO`, esse já existia no domínio desde o item 2).

Também corrigido um bug real no `ExtracaoFaturaRealManualTest`: sem
`FATURA_TESTE_ARQUIVO` explícito, ele pegava o primeiro PDF em ordem
alfabética de `test-data/` — com múltiplas faturas de bancos diferentes
lá agora, isso quebrava `mvn test` sempre que o primeiro arquivo exigia
senha e nenhuma senha estava configurada. Agora exige escolha explícita
do arquivo, senão pula de verdade (self-skip).

58 testes no total do `document-service`, suite inteira verde. Consumer
Kafka no `transaction-service` (item 9) ainda não existe — o evento é
publicado mas ninguém consome ainda.

Criado: `docs/architecture/adr` nenhum novo (mudança de spec, não de
arquitetura); `ConfirmarLancamentosCommand`, `ConfirmarLancamentosUseCase`,
`DocumentoEventPublisher` (porta) + `DocumentoEventPublisherImpl`,
`DocumentoLancamentosConfirmadosEvento`, `LancamentoConfirmadoPayload`,
`NenhumLancamentoSelecionadoException` + mapper,
`DocumentoAindaNaoProcessadoExceptionMapper`, `ConstraintViolationExceptionMapper`,
DTOs (`ConfirmarLancamentosRequest`, `ErroValidacaoResponse`).
Alterado: `docs/specs/document-service.yaml` (`contaId` no confirmar),
`LancamentoExtraidoDto`/`AgenteExtracaoFaturaService` (parsing de data
"DD MES"), `ExtracaoFaturaRealManualTest` (self-skip corrigido),
`docs/tasks.md` (item 8 detalhado + achados Itaú/Nubank).

Também nessa sessão: usuário reportou problema de acentuação no
`application.properties` do `card-service`. Investigado — nenhum arquivo
do repositório tem encoding realmente corrompido (varredura completa
confirmou UTF-8 válido em tudo); causa era o IntelliJ ter um encoding
default separado pra arquivos `.properties` (`ISO-8859-1`, herança do
`java.util.Properties` legado). Corrigido com `.idea/encodings.xml`
(local, gitignored, força UTF-8 no projeto todo) e `.editorconfig` novo
na raiz (commitado, `charset = utf-8` geral).

## 2026-08-09 — item 9: consumer Kafka no `transaction-service` (fecha o fluxo de ponta a ponta)

Pedido: "vamos para o item 9" — o último passo do fluxo desenhado em
`overview.md` seção 3, consumir o evento que o document-service publica e
criar a `Transacao` de verdade.

Achado real ao desenhar isso (documentado em
[ADR-0025](architecture/adr/0025-confirmacao-posse-conta-antes-do-evento.md)):
o padrão já usado pra debitar conta (`AccountServiceClient.debitar()` em
`transaction-service`) confirma posse da conta propagando o **token do
usuário do request HTTP recebido**. Um consumer Kafka não tem requisição
em andamento — não existe token pra propagar, e o endpoint `GET /contas/{id}`
do account-service é `@RolesAllowed("usuario")` só (não aceita role
`service`), então nem dava pra trocar pelo cliente de serviço direto.

Resolvido movendo a verificação de posse pra **um lugar só**: dentro de
`ConfirmarLancamentosUseCase` no document-service (item 8), que roda numa
requisição HTTP síncrona com o token do usuário disponível — mesmo padrão
de dois tokens já usado em card-service/transaction-service
(`AccountServiceUsuarioClient` + `PropagarAutorizacaoHeadersFactory`).
Isso exigiu dar ao `document-service` sua primeira integração de verdade
com o `account-service` (só `confirmarPosseDaConta`, nada mais — não é
integração com card-service, isso continua fora de escopo, ADR-0023). O
consumer Kafka no transaction-service **não reverifica** — confia
integralmente na checagem já feita, e vai direto pro débito/crédito via
dois métodos novos na porta `AccountServiceClient`
(`debitarSemConfirmarPosse`/`creditarSemConfirmarPosse`, sem a etapa de
`GET /contas/{id}`).

Implementado: `DocumentoLancamentosConfirmadosConsumer` (`@Incoming`),
`ProcessarLancamentosConfirmadosUseCase` (cria uma `Transacao` por
lançamento confirmado, débito/crédito real síncrono com account-service),
DTOs espelhando o payload do document-service (serviços diferentes não
compartilham código, mesmo formato JSON) e um
`ObjectMapperDeserializer` (primeiro consumer Kafka do projeto).

Validado com um teste de integração que publica um evento JSON real
direto no tópico via Kafka Dev Services (o mesmo broker que o consumer
real está ouvindo) — não só teste unitário com mock, a fiação inteira
(deserializer → consumer → caso de uso → débito mockado no
account-service → `Transacao` persistida) foi exercitada de verdade.
Achado técnico no caminho: ler um repositório dentro do corpo de um teste
(fora de uma requisição HTTP) também precisa de contexto/transação
explícitos (`QuarkusTransaction.requiringNew().call(...)`) — mesma
família do `ContextNotActiveException` já visto no document-service com
`ManagedExecutor`, dessa vez do lado do teste, não da aplicação.

Isso fecha o fluxo de ponta a ponta descrito em `overview.md` seção 3:
upload de fatura → extração via LLM → confirmação (com verificação de
posse de conta) → evento Kafka → consumer → `Transacao` criada com
débito real no `account-service`. 97 testes no `transaction-service`
(14 novos), suite inteira verde.

Criado: `docs/architecture/adr/0025-confirmacao-posse-conta-antes-do-evento.md`;
`document-service`: `AccountServiceClient` (porta) + impl,
`AccountServiceUsuarioClient`, `PropagarAutorizacaoHeadersFactory`,
`ContaNaoEncontradaException` + mapper; `transaction-service`:
`DocumentoLancamentosConfirmadosConsumer`,
`ProcessarLancamentosConfirmadosUseCase`,
`DocumentoLancamentosConfirmadosEvento`/`LancamentoConfirmadoPayload`,
`DocumentoLancamentosConfirmadosDeserializer`. Alterado:
`ConfirmarLancamentosUseCase` (chama `confirmarPosseDaConta` antes de
publicar), `AccountServiceClient`/`AccountServiceClientImpl` do
transaction-service (métodos `SemConfirmarPosse`),
`docs/specs/document-service.yaml` (404 novo no confirmar),
`application.properties` dos dois serviços, `pom.xml` do
transaction-service (Awaitility), `docs/tasks.md` (item 9 detalhado).

## 2026-08-09 — item 11: fecha a fatia 3 (docker-compose, CI, Keycloak, Postman, diagramas)

Pedido: "vamos para o item 11" — a parte de "arrumar a casa" antes de
considerar o `document-service` pronto: infra, CI, documentação viva.

`docker-compose.yml` ganhou o serviço `document-service` (depende de
mysql/mongo/kafka/ollama/account-service) — subido e validado de verdade
contra a stack real: build limpo, start sem erro, Flyway/Mongo/Kafka
todos conectando certo, healthcheck 200. `document_db` criado (script de
init + aplicado direto no container MySQL já rodando, mesmo padrão das
fatias anteriores — o script só roda em volume novo).

Keycloak: client `document-service` registrado com `bearerOnly: true`,
**sem** service-account — diferente dos outros três serviços, porque
`document-service` nunca chama outro serviço via client_credentials (só
propaga o token do próprio usuário pro `account-service`, ADR-0025), então
não precisa de client secret nem client_credentials habilitado. Aplicado
também no realm já importado no container (mesmo motivo de sempre: o
JSON só é lido no primeiro boot do Keycloak).

CI: job `document-service` novo em `ci.yml`, mesmo template dos outros
três (testes + cobertura JaCoCo + build de imagem Docker de validação +
OWASP Dependency-Check), `document-service` adicionado ao filtro de paths
do job `changes`. Não rodei o dependency-check localmente (a `NVD_API_KEY`
só existe como secret do GitHub, nunca guardada localmente por design) —
fica pro CI validar quando for commitado/pushado.

Postman: os 4 endpoints do `document-service` documentados em
`mudancas-manuais.txt` (upload multipart, listagem, detalhe/polling,
confirmar) + `document_service_url` no environment.

Documentação viva atualizada pra refletir a fatia entregue, não mais
planejada: `diagrams.md` (container graph sem mais "(planejado)", aresta
síncrona nova pro account-service, seção 3.4 nova com o ER conceitual de
`DocumentoImportado`/`LancamentoPendente`), `overview.md` (tabela de
serviços + o diagrama de sequência da seção 3 reescritos pra refletir o
fluxo assíncrono de verdade — upload 202 + polling + confirmação com
verificação de posse — não mais o desenho síncrono original de quando a
fatia começou), `roadmap.md` (fatia 3 → ✅ Entregue), `README.md` raiz
(parágrafo do serviço, tabela de endpoints, URLs de Mongo/Ollama).

Isso fecha a fatia 3 do roadmap. Escopo que ficou de fora de propósito
(não é dívida técnica esquecida, é escolha registrada em ADR-0004/0023):
extrato bancário (PDF/CSV), boleto de financiamento e ingestão por foto
via mobile — todos ficam pra uma fatia futura, quando fizer sentido
retomar.

Alterado: `docker-compose.yml`, `infra/mysql/init/01-databases.sql`,
`infra/keycloak/realm-financas.json`, `.github/workflows/ci.yml`,
`docs/postman/mudancas-manuais.txt`,
`docs/postman/financas-dev.postman_environment.json`,
`docs/architecture/diagrams.md`, `docs/architecture/overview.md`,
`docs/roadmap.md`, `README.md`, `docs/tasks.md` (itens 10–11, fatia 3
fechada).

## 2026-08-10 — CI do document-service: falso positivo do OWASP Dependency-Check

Pedido: "vamos preparar o commit agora" e depois "sim, pode fazer push" —
ao subir o `document-service` pela primeira vez pro GitHub, o CI pegou de
verdade um achado que não tinha como testar localmente (a scan real
depende da `NVD_API_KEY`, que só existe como secret do GitHub).

O job `document-service` falhou no passo "Scan de vulnerabilidade (OWASP
Dependency-Check)": `quarkus-mongodb-client`/`quarkus-mongodb-panache`
(dependência direta pra Mongo Panache, ADR-0005, mais a transitiva dela)
batiam com o CPE `cpe:2.3:a:mongodb:mongodb:3.38.1` — o Dependency-Check
interpretando "3.38.1" (versão do Quarkus) como se fosse uma versão do
produto **MongoDB Server**, que nem existe nessa numeração. Falso
positivo clássico de CPE mal atribuído a uma versão coincidente, não uma
vulnerabilidade real (CVE-2021-32036/2025-14847/2026-9753/2014-8180,
CVSS até 8.7 — acima do gate `failBuildOnCVSS=7` do ADR-0017).

Levou 4 pushes pra sair do vermelho, cada um corrigindo um erro diferente
na sintaxe do arquivo de supressão (`dependency-check-suppression.xml`,
novo, com `<suppressionFiles>` wireado no `pom.xml`):

1. Regra por `<packageUrl>` + `<cpe regex="true">` — deu "zero matches"
   no log do CI mesmo com o CPE aparentemente batendo. Causa: quando
   `<packageUrl>` e `<cpe>` aparecem juntos numa regra, o Dependency-Check
   exige que **todos** os CPEs daquela dependência batam com o filtro —
   e o artefato também carrega o CPE correto `quarkus:quarkus`, que não
   bate com um filtro só de `mongodb:mongodb`. Trocado pra suprimir por
   `<cve>ID</cve>` exato (mais robusto, não depende de combinação de
   CPEs).
2. `<packageUrl>` mirava em `quarkus-mongodb-client`, mas o achado da vez
   era `quarkus-mongodb-panache` — nome de artefato errado, supressão
   continuou sem efeito.
3. Corrigido pra `quarkus-mongodb-panache`, mas o achado voltou a ser
   `quarkus-mongodb-client` no scan seguinte — os dois JARs existem de
   verdade (panache é dependência direta, client é transitiva dele) e
   **os dois** carregam a mesma CPE incorreta; cada correção só tapava um
   buraco e destapava o outro.
4. Regex ampliado pra `quarkus-mongodb-.*`, cobrindo os dois artefatos de
   uma vez — CI finalmente verde: testes, cobertura, build de imagem
   Docker e o scan de vulnerabilidade todos passando, com
   `account-service`/`transaction-service`/`card-service` corretamente
   pulados pelo `paths-filter` (nenhum arquivo deles mudou).

Lição registrada na nota de supressão (pra não repetir): supressão por
CVE exato é mais confiável que por CPE quando o artefato carrega múltiplos
CPEs; e ao suprimir por nome de artefato, checar se a dependência tem
variantes/transitivas com o mesmo problema antes de considerar resolvido.

Criado: `services/document-service/dependency-check-suppression.xml`.
Alterado: `services/document-service/pom.xml` (wireia o
`suppressionFiles`).

## 2026-08-10 — fatia 4: spec do budget-service

Pedido: "pode começar o budget-service, cria a spec primeiro".

Antes de escrever a spec foi preciso fechar a regra exata de "disponível
pra gastar" (PRD 3.3 deixa isso propositalmente em aberto, delegado pro
`budget-service` decidir e documentar) — registrado em
[ADR-0026](architecture/adr/0026-regra-calculo-disponivel-para-gastar.md):

```
disponivelParaGastar(mês) =
    Σ saldo das contas CORRENTE/CARTEIRA (account-service)
  − Σ valorTotal das faturas FECHADA com vencimento no mês (card-service)
  − Σ valor das despesas recorrentes ATIVA com dataInicio <= fim do mês (transaction-service)
  − reserva (valor único, definido no budget-service)
```

Decisões que ficaram registradas na ADR, pra não perder o porquê: só conta
CORRENTE/CARTEIRA entram (poupança/investimento têm fricção pra resgatar,
não são "disponível" no sentido do PRD); fatura só conta se `FECHADA` (não
`ABERTA` — valor ainda não é definitivo, compras continuam entrando até o
fechamento); despesa recorrente é aproximada como "regra ATIVA = 1
compromisso fixo por mês", sem tentar prever se a ocorrência do mês já foi
materializada pelo `transaction-service` — esse contrato não está
documentado/estável ainda, duplicar esse cálculo de data seria mais
frágil que a aproximação simples (que também é o modelo mental mais
natural: "meu aluguel é R$X todo mês", independente do sistema já ter
lançado a transação ou não). A resposta do endpoint devolve o
detalhamento item a item de cada parcela (contas, faturas, despesas
recorrentes), não só o total — requisito direto do PRD seção 6 (a IA
precisa conseguir explicar de onde tirou o número).

Spec (`docs/specs/budget-service.yaml`) ficou com dois grupos de endpoint
independentes: orçamento por categoria/mês (`POST/GET /orcamentos`,
`PUT/DELETE /orcamentos/{id}`, unique por usuarioId+categoria+mesReferencia,
`valorConsumido` calculado chamando `transaction-service`
`GET /transacoes/resumo-por-categoria`, endpoint que já existe) e o
cálculo de disponível pra gastar (`GET /disponivel-para-gastar` +
`GET/PUT /reserva`) — funcionalidades relacionadas mas que não
compartilham cálculo entre si. Três integrações de saída novas (todas
leitura, token do usuário propagado — mesmo padrão de dois tokens do
ADR-0025): `account-service`, `card-service`, `transaction-service`.

`docs/tasks.md` ganhou a seção da fatia 4 (item 1 = spec, concluído; itens
2–7 = scaffold/domínio/clientes/casos de uso/REST/fechamento, ainda não
iniciados). `docs/roadmap.md`: fatia 4 → 🔶 Em andamento.

Criado: `docs/specs/budget-service.yaml`,
`docs/architecture/adr/0026-regra-calculo-disponivel-para-gastar.md`.
Alterado: `docs/tasks.md` (fatia 4 detalhada), `docs/roadmap.md` (status
+ próxima ação concreta).

## 2026-08-10 — item 2: scaffold do budget-service

Pedido: "pode seguir, cria o scaffold do budget-service".

Mesmo padrão dos scaffolds anteriores (copiado de `card-service`, ajustado
pro serviço novo): `pom.xml`, `mvnw`/`mvnw.cmd`, Dockerfiles (`jvm`/
`legacy-jar`/`native`/`native-micro`), `application.properties` (porta
8085), `.gitignore`/`.dockerignore`, `README.md`. `./mvnw compile`
validado.

Duas diferenças de propósito em relação ao `card-service`, ambas ligadas
ao ADR-0026 (todas as três chamadas de saída do `budget-service` propagam
o token do próprio usuário, nenhuma usa role `service`): `pom.xml` sem
`quarkus-scheduler` (não tem job agendado nessa fatia) e sem
`quarkus-rest-client-oidc-filter`; `application.properties` sem bloco
`quarkus.oidc-client`/`client_credentials` — mesmo padrão "bearer only,
sem service account" já usado no Keycloak do `document-service`
(ADR-0025), só que aqui vale pra três integrações de saída ao mesmo tempo,
não uma. Três blocos de REST client já configurados (account-service
`:8081`, card-service `:8083`, transaction-service `:8082`), ainda sem
interface Java nenhuma — só a config.

`overview.md`: linha do `budget-service` na tabela de serviços →
🔶 Em andamento.

Criado: `services/budget-service/` (scaffold completo, sem
domínio/persistência/REST ainda — próximo passo é o item 3 do backlog em
`docs/tasks.md`). Alterado: `docs/tasks.md` (item 2 concluído),
`docs/architecture/overview.md` (status do serviço).

## 2026-08-10 — item 3: domínio e persistência do budget-service

Pedido: "pode seguir pro item 3".

`Orcamento` (aggregate root) e `Reserva` — copiando de perto o padrão já
estabelecido em `Cartao`/`Fatura` do `card-service` (factory `criar()` com
validação, `reconstituir()` pra hidratar da persistência sem revalidar,
mutação por método de domínio nunca por setter solto). Duas decisões que
vale registrar (nenhuma virou ADR — são detalhes de implementação dentro
da regra já fechada em ADR-0026, não decisão de arquitetura nova):

- `Orcamento.mesReferencia` é `YearMonth`, não `String` — mesmo tipo já
  usado em `Fatura.competencia` do card-service, convertido pra
  `String` (formato AAAA-MM) só na borda da persistência
  (`OrcamentoMapper`), mesma técnica.
- `valorConsumido`/`valorDisponivel` (que aparecem no
  `OrcamentoResponse` da spec) **não** entram no domínio nem são
  persistidos — são calculados na hora pelo caso de uso de listagem
  (item 5), chamando `transaction-service`. O domínio só guarda o que é
  fonte de verdade (`valorLimite`), nunca um valor derivado que ficaria
  desatualizado.
- Duplicata de orçamento (mesma categoria+mês, usuário) é rejeitada em
  código (`OrcamentoRepository.existeAtivo`, checado pelo caso de uso de
  criação), não por `UNIQUE` no banco — motivo: cancelar um orçamento e
  criar outro pra mesma categoria/mês no futuro é fluxo válido (orçamento
  cancelado não deveria bloquear um novo), e um `UNIQUE` simples em
  (usuario_id, categoria, mes_referencia) bloquearia isso incluindo
  cancelados. Ficou só um índice não-único pra performance de consulta.

`Reserva` foge do molde de aggregate — é config de valor único por
usuário (`usuarioId` é a própria chave primária na tabela, sem id
gerado à parte), sempre upsert, sem histórico. `Reserva.semDefinir(usuarioId)`
modela o estado "usuário nunca configurou" (valor 0, `atualizadoEm`
nulo) — usado pelo caso de uso de busca (item 5) quando o repositório não
acha linha nenhuma, pra `GET /reserva` nunca precisar devolver 404 (spec
já dizia isso, agora o domínio reflete).

Persistência: `OrcamentoEntity`/`ReservaEntity` + mapper + Panache
repository + `*RepositoryImpl`, mesmo padrão exato de
`CartaoEntity`/`CartaoMapper`/`CartaoPanacheRepository`/
`CartaoRepositoryImpl`. Duas migrations Flyway novas
(`V1__create_orcamentos_table.sql`, `V2__create_reservas_table.sql`).

12 testes de domínio (`OrcamentoTest`, `ReservaTest`), `./mvnw test`
verde. Sem teste de integração de banco ainda — mesmo padrão já usado no
`document-service` (item 2, 2026-08-09): fica pra ser exercida pelos
testes REST do item 6, quando o serviço já tiver endpoint pra testar de
ponta a ponta, em vez de duplicar setup de Testcontainers cedo demais.

Criado: `services/budget-service/src/main/java/.../domain/` (`Orcamento`,
`StatusOrcamento`, `OrcamentoRepository`, `OrcamentoNaoEncontradoException`,
`OrcamentoJaExisteException`, `Reserva`, `ReservaRepository`),
`.../infrastructure/persistence/` (`OrcamentoEntity`/`Mapper`/
`PanacheRepository`/`RepositoryImpl`, `ReservaEntity`/`Mapper`/
`PanacheRepository`/`RepositoryImpl`), `.../resources/db/migration/`
(V1, V2), testes de domínio. Alterado: `docs/tasks.md` (item 3
concluído).

## 2026-08-10 — item 4: clientes de saída do budget-service

Pedido: "pode seguir pro item 4".

Três portas novas em `domain/` (`AccountServiceClient`, `CardServiceClient`,
`TransactionServiceClient`) + implementação REST em `infrastructure/client/`
— achado que simplificou o design em relação ao padrão já visto em
card-service/document-service: nenhum dos cinco endpoints chamados aqui
(`GET /contas`, `GET /cartoes`, `GET /cartoes/{id}/faturas`,
`GET /transacoes-recorrentes`, `GET /transacoes/resumo-por-categoria`)
recebe um id de entrada pra confirmar posse — todos já filtram pelo `sub`
do token no próprio servidor. Isso eliminou a necessidade do padrão de
dois clientes por integração (`*UsuarioClient` pra posse +
`*InternoClient` pra ação, visto em card-service/document-service): aqui
é só propagação de token, `PropagarAutorizacaoHeadersFactory` único,
compartilhado pelos três `@RegisterRestClient`.

`CardServiceClientImpl` é o único que orquestra mais de uma chamada:
card-service não expõe "todas as faturas em aberto do usuário" — o
cliente lista os cartões ativos primeiro, depois busca as faturas
`FECHADA` de cada um, achatando o resultado. Decisão de onde filtrar por
mês: nos clientes, não — `AccountServiceClient`/`CardServiceClient`/
`TransactionServiceClient` devolvem os dados brutos (todas as contas,
todas as faturas fechadas de qualquer mês, todas as despesas recorrentes
ativas); o filtro por `dataVencimento`/`dataInicio` dentro do mês
consultado é responsabilidade do caso de uso (item 5) — os clientes não
têm opinião sobre "qual mês", só sabem buscar o dado.

Cada porta ganhou seu próprio tipo de retorno no domínio (`Conta`,
`FaturaFechada`, `DespesaRecorrente`, `ResumoCategoria` — records
simples, só os campos que o cálculo do ADR-0026 precisa), separados dos
DTOs de infraestrutura (`ContaDto`/`CartaoDto`/`FaturaDto`/
`TransacaoRecorrenteDto`/`ResumoCategoriaDto`) que espelham só o
subconjunto necessário da resposta real de cada serviço — mesmo
princípio já usado em `ContaDto` do card-service ("só os campos que este
serviço precisa").

Sem teste dedicado pros três `*ClientImpl` — mesmo padrão já estabelecido
no projeto (`AccountServiceClientImpl` do card-service, `OllamaLlmProvider`
do document-service): thin adapter, mockado nos testes de quem usa
(caso de uso no item 5, REST no item 6), nunca testado contra o serviço
real em CI (`testing-strategy.md` seção 2). `./mvnw compile`/`test`
validados, 12 testes de domínio continuam verdes (nada quebrou).

Criado: `services/budget-service/src/main/java/.../domain/`
(`Conta`, `AccountServiceClient`, `FaturaFechada`, `CardServiceClient`,
`DespesaRecorrente`, `ResumoCategoria`, `TransactionServiceClient`),
`.../infrastructure/client/` (`PropagarAutorizacaoHeadersFactory`,
`AccountServiceRestClient`/`ClientImpl`, `CardServiceRestClient`/
`ClientImpl`, `TransactionServiceRestClient`/`ClientImpl`, `dto/`).
Alterado: `docs/tasks.md` (item 4 concluído).

## 2026-08-10 — item 5: casos de uso do budget-service

Pedido: "pode seguir pro item 5".

Sete casos de uso em `application/`: `CriarOrcamentoUseCase`,
`AtualizarOrcamentoUseCase`, `ExcluirOrcamentoUseCase`,
`ListarOrcamentosUseCase`, `DefinirReservaUseCase`, `BuscarReservaUseCase`
e `CalcularDisponivelParaGastarUseCase` — esse último é onde a fórmula do
ADR-0026 finalmente vira código, depois de três sessões só preparando o
terreno (spec, domínio, clientes).

`OrcamentoDetalhe` (application, `record Orcamento + valorConsumido`) é o
mesmo molde de `FaturaDetalhe` do card-service — combina o aggregate com
um valor calculado na hora, sem persistir o calculado. Achado de
eficiência ao implementar `ListarOrcamentosUseCase`: em vez de perguntar
ao `transaction-service` o gasto de cada orçamento individualmente (N
chamadas pra N orçamentos do mês), busca o resumo por categoria **uma
vez só pro mês inteiro** e monta um mapa categoria→gasto reaproveitado —
testado explicitamente (`naoDeveriaChamarTransactionService_quandoNenhumOrcamentoAtivo`
verifica que o cliente nem é chamado se a lista vier vazia, e o teste de
listagem com dois orçamentos verifica só uma chamada ao resumo).

`CalcularDisponivelParaGastarUseCase` é onde o filtro por mês (que os
clientes do item 4 deixaram propositalmente de fora) finalmente
acontece: contas filtradas por `tipo` (`CORRENTE`/`CARTEIRA`, poupança/
investimento excluídas), faturas filtradas por `dataVencimento` dentro
do mês consultado, despesas recorrentes filtradas por `dataInicio <=` fim
do mês. Reserva vem do próprio repositório local (`ReservaRepository`,
não é chamada de outro serviço). Resultado (`DisponivelParaGastarResultado`)
carrega os totais E as listas completas de cada parcela (contas, faturas,
despesas recorrentes) — a spec já previa isso (campo `detalhamento`),
aqui é onde os dados pra preencher esse campo são de fato calculados.

30 testes no total (18 novos, incluindo um teste que exercita a fórmula
completa com números reais — 3 contas de tipos diferentes, 2 faturas em
meses diferentes, 2 despesas recorrentes com `dataInicio` diferentes —
verificando que só o que deveria entrar no cálculo entra), `./mvnw test`
verde.

Criado: `services/budget-service/src/main/java/.../application/`
(`OrcamentoDetalhe`, `CriarOrcamentoCommand`/`UseCase`,
`AtualizarOrcamentoCommand`/`UseCase`, `ExcluirOrcamentoUseCase`,
`ListarOrcamentosUseCase`, `DefinirReservaCommand`/`UseCase`,
`BuscarReservaUseCase`, `DisponivelParaGastarResultado`,
`CalcularDisponivelParaGastarUseCase`) + testes correspondentes.
Alterado: `docs/tasks.md` (item 5 concluído).

## 2026-08-10 — item 6: REST do budget-service (fecha a implementação)

Pedido: "pode seguir pro item 6".

Três resources em `infrastructure/rest/`: `OrcamentoResource`
(`POST/GET /orcamentos`, `PUT/DELETE /orcamentos/{id}`), `ReservaResource`
(`GET/PUT /reserva`), `DisponivelParaGastarResource`
(`GET /disponivel-para-gastar`) — todos seguindo o molde já estabelecido
(`usuarioIdAutenticado()` extrai `sub` do JWT, nunca aceita do corpo da
requisição, ADR-0003; DTOs de resposta com `static de(...)`, mesmo
padrão de `CartaoResponse`/`FaturaResponse` do card-service).

Achado técnico ao tipar o query param `mes`: `YearMonth` não funciona
como tipo de `@QueryParam` — JAX-RS só converte automaticamente tipos com
`valueOf(String)` ou `fromString(String)` estático (ou construtor de
String), e `YearMonth` só tem `parse(CharSequence)`, que não conta. Saída:
`mes` fica `String` com `@Pattern` validando o formato `AAAA-MM` (mesmo
regex da spec), convertido pra `YearMonth.parse(mes)` manualmente dentro
do método. Já no *corpo* das requisições (`CriarOrcamentoRequest.mesReferencia`)
`YearMonth` funciona sem nenhum ajuste — Jackson desserializa via
`JavaTimeModule`, já ativo no projeto por causa de `Instant`/`LocalDate`
usados nos outros serviços (mesma dependência, sem configuração nova).

`OrcamentoResponse.de(OrcamentoDetalhe)` é onde `valorDisponivel`/
`percentualConsumido` são finalmente calculados — na borda REST, não no
caso de uso (item 5 só devolve `Orcamento` + `valorConsumido` cru,
consistente com a divisão já usada em `FaturaDetalhe`/`FaturaResponse`
do card-service: aggregate + dado calculado no `application`, derivação
puramente aritmética/apresentacional no `infrastructure/rest`).

19 testes de integração novos (`@QuarkusTest` + RestAssured + `QuarkusMock`
pros três `*ClientImpl`, mesmo padrão de `CartaoResourceTest`). Dois
achados durante os testes, ambos já vistos antes no projeto mas
reencontrados aqui:
- Fixture de `sub` de teste com caractere fora do alfabeto hexadecimal
  (`"b0dge7000..."`, o `g` não é hex) quebrava `UUID.fromString` — trocado
  por um valor hex válido (`"b0d9e700..."`).
- Dois testes que criavam orçamento "Mercado"/2026-08 pro mesmo usuário
  de um teste anterior (mesma classe, sem rollback automático entre
  métodos — lição já registrada desde a fatia 2/`CartaoResourceTest`)
  colidiam com a checagem de duplicata (`OrcamentoJaExisteException`,
  422 em vez do 201/200 esperado) — corrigido com `sub` dedicado por
  teste que precisa de estado isolado.
- `BigDecimal.ZERO` serializa como `0` no JSON, não `0.0` — asserções
  Hamcrest com `equalTo(0.0f)` falhavam contra o `0` inteiro devolvido
  pela API (`Reserva.semDefinir`/`disponivelParaGastar` sem nenhum dado).

49 testes no total do `budget-service` (30 anteriores + 19 novos),
`./mvnw test` verde — **fecha a implementação da fatia 4** (só falta o
item 7: docker-compose/CI/Keycloak/Postman/diagramas, mesmo padrão do
item 11 da fatia 3).

Criado: `services/budget-service/src/main/java/.../infrastructure/rest/`
(`OrcamentoResource`, `ReservaResource`, `DisponivelParaGastarResource`,
`OrcamentoNaoEncontradoExceptionMapper`, `OrcamentoJaExisteExceptionMapper`,
`ConstraintViolationExceptionMapper`, `dto/` — `CriarOrcamentoRequest`,
`AtualizarOrcamentoRequest`, `OrcamentoResponse`, `DefinirReservaRequest`,
`ReservaResponse`, `DisponivelParaGastarResponse`,
`DetalhamentoDisponivel`, `ContaResumo`, `FaturaResumo`,
`DespesaRecorrenteResumo`, `ErroResponse`, `ErroValidacaoResponse`) +
testes de integração. Alterado: `docs/tasks.md` (item 6 concluído).

## 2026-08-10 — item 7: fecha a fatia 4 (docker-compose, CI, Keycloak, Postman, diagramas)

Pedido: "pode seguir pro item 7" — mesmo padrão de fechamento já usado no
item 11 da fatia 3 (`document-service`).

`docker-compose.yml` ganhou o serviço `budget-service` (depende de mysql/
account-service/card-service/transaction-service) — subido e validado de
verdade contra a stack real: `./mvnw package -DskipTests` +
`docker compose up -d --build budget-service`, build limpo, Flyway
aplicou as duas migrations (`orcamentos`/`reservas`) na subida,
`GET /q/health` → 200. `budget_db` criado (script de init + aplicado
direto no container MySQL já rodando, mesmo padrão das fatias
anteriores).

Keycloak: client `budget-service` registrado com `bearerOnly: true`,
**sem** service-account — mesmo motivo do `document-service` (ADR-0025),
generalizado aqui pras três integrações de saída do `budget-service`
(ADR-0026): nenhuma delas usa client_credentials, todas propagam o token
do próprio usuário, então `bearerOnly: true` é suficiente. Aplicado
também no realm já importado no container rodando (`kcadm.sh create
clients`, mesmo comando das vezes anteriores).

CI: job `budget-service` novo em `ci.yml`, mesmo template dos outros
quatro (testes + cobertura JaCoCo + build de imagem Docker de validação +
OWASP Dependency-Check), `budget-service` adicionado ao filtro de paths
do job `changes`. Não rodei o dependency-check localmente (mesmo motivo
de sempre — `NVD_API_KEY` só existe como secret do GitHub); se aparecer
outro falso positivo de CPE como o do `document-service` (2026-08-10,
`quarkus-mongodb-*`), o processo de diagnóstico já está documentado nesse
histórico.

Postman: os 7 endpoints do `budget-service` documentados em
`mudancas-manuais.txt` (orçamento CRUD, reserva, disponível pra gastar)
+ `budget_service_url` no environment.

Documentação viva atualizada pra refletir a fatia entregue: `diagrams.md`
(container graph sem mais "(planejado)", três arestas síncronas novas
pros três serviços consultados, seção 3.5 nova com o ER conceitual de
`Orcamento`/`Reserva` — substituindo a seção "Pendente" que só linkava
pro roadmap —, seção 4.6 nova com o diagrama de classes do domínio,
mesmo nível de detalhe das seções 4.1–4.5 dos outros serviços);
`overview.md` (tabela de serviços → ✅ Entregue, seção 6 nova com o
diagrama de sequência do fluxo "disponível pra gastar" — as três
chamadas síncronas de leitura e onde o filtro por mês acontece —, seções
seguintes renumeradas); `roadmap.md` (fatia 4 → ✅ Entregue, próxima ação
aponta pra fatia 5); `README.md` raiz (parágrafo do serviço, tabela de
endpoints, contagem de serviços no CI/docker compose de quatro pra
cinco).

Isso fecha a fatia 4 do roadmap — `budget-service` entregue de ponta a
ponta: spec (item 1) → ADR-0026 (regra de cálculo) → domínio/persistência
(item 3) → clientes de saída (item 4) → casos de uso (item 5) → REST
(item 6) → infra/CI/documentação (item 7). 49 testes, CI configurado,
container validado contra a stack real.

Alterado: `docker-compose.yml`, `infra/mysql/init/01-databases.sql`,
`infra/keycloak/realm-financas.json`, `.github/workflows/ci.yml`,
`docs/postman/mudancas-manuais.txt`,
`docs/postman/financas-dev.postman_environment.json`,
`docs/architecture/diagrams.md`, `docs/architecture/overview.md`,
`docs/roadmap.md`, `README.md`, `docs/tasks.md` (item 7, fatia 4
fechada).

## 2026-08-10 — fatia 5: Qdrant confirmado e spec do ai-service

Pedido: "pode seguir pro item 1 do ai-service".

Antes de escrever a spec, uma pendência real bloqueava: ADR-0005 (Qdrant
como vector store) estava desde 2026-08-06 marcada como "proposta, não
confirmada — decisão minha, revisar antes de implementar o ai-service".
Perguntei ao usuário; resposta: **Qdrant confirmado** (opção
recomendada, mantendo o plano já documentado em `ai-strategy.md`).
ADR-0005 → `Aceita`.

Spec (`docs/specs/ai-service.yaml`) ficou com 4 endpoints:
`POST /chat`, `GET /conversas`, `GET /conversas/{id}`,
`GET/PUT /configuracao`. Duas decisões de desenho tomadas ao escrever,
nenhuma delas prevista em ADR anterior:

- **Um único endpoint pra tudo o que acontece na conversa**
  (`POST /api/v1/chat`): pergunta nova, comando de ação, correção de uma
  proposta pendente e a confirmação em si ("sim"/"confirmar") passam
  todos pelo mesmo endpoint — o agente orquestrador decide a intenção
  pelo texto + estado da conversa (`ai-strategy.md` seção 4.2 já
  descrevia esse fluxo, só não explicitava que seria tudo num endpoint
  só). Diferente do padrão do `document-service`, que usa um
  `POST /confirmar` estruturado com ids explícitos — aqui a confirmação
  é conversacional por natureza (PRD 3.5 é explícito: comando falado ou
  escrito, resposta em linguagem natural), então um endpoint separado
  não fazia sentido.
- **Configuração de IA por usuário** (`GET/PUT /api/v1/configuracao`) é
  decisão nova: `ai-strategy.md` diz "cada usuário escolhe o seu
  [provedor]" — diferente do `document-service`, que usa um Ollama único
  pra todos via variável de ambiente (`OLLAMA_BASE_URL`/`OLLAMA_MODEL`).
  Como nenhum outro serviço no sistema é dono de "preferência do
  usuário" (não existe um `user-service` — Keycloak cuida só de
  identidade), esse dado fica no próprio `ai-service`. `apiKey` nunca é
  devolvida em texto claro na resposta, só um booleano `configurado`.

As tools MCP (tabela já em `ai-strategy.md` seção 4) ficaram de fora do
contrato OpenAPI de propósito — MCP é um protocolo próprio, não REST; a
spec cobre só a superfície voltada ao cliente web/mobile (chat,
histórico, configuração).

`docs/tasks.md` ganhou a seção da fatia 5 (item 1 = spec + confirmação
do Qdrant, concluído; itens 2–10 = scaffold/Qdrant no compose/domínio/
LlmProvider próprio/clientes de saída/RAG/agente orquestrador/REST/
fechamento — backlog mais longo que as fatias anteriores, reflete a
complexidade nova: primeiro serviço com vector store, chat, e agente).
`docs/roadmap.md`: fatia 5 → 🔶 Em andamento. `overview.md`: linha do
`ai-service` na tabela de serviços → 🔶 Em andamento.

Criado: `docs/specs/ai-service.yaml`. Alterado:
`docs/architecture/adr/0005-vector-store-qdrant.md` (Proposta → Aceita),
`docs/architecture/ai-strategy.md` (nota de abertura), `docs/tasks.md`
(fatia 5 detalhada), `docs/roadmap.md`, `docs/architecture/overview.md`
(status do serviço).

## 2026-08-10 — item 2: scaffold do ai-service

Pedido: "pode seguir pro item 2".

Mesmo padrão dos scaffolds anteriores (copiado de `document-service`
dessa vez, não `card-service` — é o serviço mais parecido: sem MySQL,
com MongoDB): `pom.xml`, `mvnw`/`mvnw.cmd`, Dockerfiles, `application.properties`
(porta 8086), `.gitignore`/`.dockerignore`, `README.md`. `./mvnw compile`
validado.

Diferença mais notável em relação a todos os outros quatro serviços:
`pom.xml` **sem** `quarkus-hibernate-orm`/`quarkus-hibernate-orm-panache`/
`quarkus-jdbc-mysql`/`quarkus-flyway` — `ai-service` é o primeiro serviço
do sistema sem banco relacional próprio (só MongoDB pro histórico de
conversas + Qdrant pros embeddings, ver `overview.md`); também sem
Kafka (não publica nem consome evento nessa fatia).

Ajuste em relação ao plano do item 1: o cliente Qdrant, que a descrição
original do item 2 previa entrar já no scaffold, ficou de fora do
`pom.xml` por ora — vai entrar no item 7 (RAG), quando o código que
realmente usa a dependência existir, mesmo critério já aplicado no
scaffold do `document-service` (não adicionar dependência não usada
ainda). Pelo mesmo motivo, `application.properties` só tem MongoDB +
OIDC (sem service account — `ai-service` também só propaga token do
usuário pros serviços que consulta, nunca client_credentials, mesmo
padrão do `budget-service`/ADR-0026) + porta HTTP; os REST clients de
saída (Ollama/OpenAI pro `LlmProvider`, e os quatro serviços das tools
MCP) entram junto da implementação de cada um, itens 5 e 6.

Criado: `services/ai-service/` (scaffold completo, sem
domínio/persistência/REST ainda — próximo passo é o item 3 do backlog em
`docs/tasks.md`, Qdrant no `docker-compose.yml`). Alterado:
`docs/tasks.md` (item 2 concluído), `docs/architecture/overview.md`
(status do serviço).

## 2026-08-10 — item 3: Qdrant no docker-compose.yml

Pedido: "pode seguir pro item 3".

Serviço `qdrant` novo (`qdrant/qdrant:latest`), agrupado com os outros
bancos no `docker-compose.yml` (junto de `mongo`, antes do `ollama`) —
porta `6333` (REST + dashboard web em `/dashboard`) e `6334` (gRPC, o
que o cliente Java vai usar quando o item 7 chegar), volume
`qdrant-data` novo. Validado subindo de verdade: `docker compose up -d
qdrant` → start limpo, log confirma HTTP na 6333 e gRPC na 6334,
`curl http://localhost:6333/` devolveu 200 com a versão (`1.19.0`).

`README.md`: duas linhas novas na tabela de URLs (dashboard + REST/gRPC)
— `docs/architecture/interfaces-graficas.md` não foi tocado, porque já
estava desatualizado antes desta sessão (nunca ganhou entradas de
card-service/document-service/budget-service também — dívida
pré-existente, fora do escopo de corrigir agora).

Criado: nada em código ainda (só infra). Alterado: `docker-compose.yml`
(serviço `qdrant` + volume), `README.md` (URLs), `docs/tasks.md` (item 3
concluído).

## 2026-08-10 — item 4: domínio e persistência do ai-service

Pedido: "pode seguir pro item 4".

`Conversa` (aggregate root) com `Mensagem` e `AcaoPendente` **embutidos**
— nenhum dos dois tem coleção própria, ao contrário do split usado nos
outros agregados MongoDB+MySQL deste sistema (ex.
`DocumentoImportado`/`LancamentoPendente` no document-service). Faz
sentido porque `ai-service` só tem MongoDB: "um documento por conversa,
com o array de mensagens dentro" é literalmente o desenho canônico do
banco pra esse tipo de dado, sem precisar de referência lógica entre
coleções.

Decisão nova, não estava em nenhum ADR/doc anterior: `AcaoPendente.propor()`
fixa `expiraEm = agora + 10 minutos`. ADR-0007 só dizia "expira depois de
um tempo curto" sem valor exato — 10 minutos foi a escolha feita agora
(prazo curto o bastante pra não confirmar sobre contexto desatualizado,
longo o bastante pra não expirar no meio de uma troca de mensagens
normal). `Conversa.confirmarAcaoPendente(agora)` sempre limpa o estado
depois de chamado, nos dois casos — sucesso (devolve a ação, quem chama
executa a mutação de verdade) ou expirada (lança exceção, mas não deixa
a proposta velha "pairando" pra tentar de novo).

`ConfiguracaoIa` segue o molde de `Reserva` do budget-service (1
documento por usuário, upsert, sem histórico), mas com uma diferença
que puxou trabalho novo: `apiKey` da OpenAI precisa ficar criptografada
no banco (`security.md` já documentava isso desde antes desta fatia
começar — "Banco do ai-service, campo criptografado"). Criado
`CriptografiaService` (AES-256/GCM, IV aleatório de 12 bytes por
chamada, prefixado ao texto cifrado antes do Base64 — não precisa
guardar o IV à parte). A chave vem de
`ai-service.criptografia.chave`/`AI_SERVICE_CRIPTOGRAFIA_CHAVE`, com um
default de dev gerado com `openssl rand -base64 32` (documentado como
dev-only no comentário do `application.properties`, mesmo padrão das
outras credenciais de dev já hardcoded no projeto, ex. `MYSQL_PASSWORD`).
Achado de design ao encaixar isso na arquitetura: os mappers estáticos
do projeto (`ConfiguracaoIaMapper`, sem construtor, só métodos
`static`) não têm acesso a bean CDI — então a criptografia/descriptografia
ficou no `ConfiguracaoIaRepositoryImpl` (que já é `@ApplicationScoped`
com injeção), não no mapper; o mapper cuida só dos campos não sensíveis.

24 testes (`ConversaTest`, `AcaoPendenteTest`, `ConfiguracaoIaTest`,
`CriptografiaServiceTest` — inclui um teste específico garantindo que o
mesmo texto plano gera criptografados diferentes a cada chamada, prova
de que o IV está sendo randomizado de verdade, não reaproveitado).
`./mvnw test` verde. Sem teste de integração de banco ainda — mesmo
padrão já usado nos outros serviços com Mongo, fica pro item 9 (REST).

Criado: `services/ai-service/src/main/java/.../domain/` (`AutorMensagem`,
`TipoRespostaAgente`, `Mensagem`, `TipoTransacao`, `FrequenciaRecorrencia`,
`AcaoPendente`, `Conversa`, `ConversaNaoEncontradaException`,
`NenhumaAcaoPendenteException`, `AcaoPendenteExpiradaException`,
`ConversaRepository`, `ProvedorIa`, `ConfiguracaoIa`,
`ConfiguracaoIaRepository`), `.../infrastructure/persistence/`
(`ConversaEntity`/`MensagemEmbedded`/`AcaoPendenteEmbedded`/`Mapper`/
`MongoRepository`/`RepositoryImpl`, `ConfiguracaoIaEntity`/`Mapper`/
`MongoRepository`/`RepositoryImpl`), `.../infrastructure/security/`
(`CriptografiaService`), testes. Alterado:
`services/ai-service/src/main/resources/application.properties`
(chave de criptografia), `docs/tasks.md` (item 4 concluído).

## 2026-08-10 — item 5: LlmProvider e adapters do ai-service

Pedido: "pode seguir pro item 5".

Porta `LlmProvider` (cópia própria do `ai-service`, mesma forma da porta
já usada no `document-service` — `chat()`/`isConfigured()` — com
`embed()` a mais, porque esse serviço precisa de RAG e o document-service
não precisava). Dois adapters: `OllamaLlmProvider` (copiado quase
literalmente do document-service, incluindo o timeout generoso de 120s
já validado na prática contra inferência local em CPU) e
`OpenAiLlmProvider` (novo — `/v1/chat/completions` e `/v1/embeddings`,
modelo `gpt-4o-mini`/`text-embedding-3-small`).

Achado de design real ao encaixar "cada usuário escolhe o seu provedor"
(ADR-0002) na porta documentada em `ai-strategy.md`, que não tem
parâmetro de config em `chat()`/`embed()`: se `LlmProvider` fosse bean
CDI fixo (padrão de todos os outros adapters do projeto até agora), não
teria como saber de qual usuário é a chamada. Resolvido com uma porta
nova, `LlmProviderFactory` (não estava prevista em nenhum ADR/doc
anterior) — recebe a `ConfiguracaoIa` do usuário e devolve um
`LlmProvider` já resolvido pro provedor/credencial certos, uma vez por
chamada. Consequência: `OllamaLlmProvider`/`OpenAiLlmProvider` **não são
bean CDI** — são instanciados via `new` dentro da factory, carregando o
estado resolvido (apiKey da OpenAI, modelo do Ollama). Os REST clients
em si continuam CDI (`@RestClient OllamaRestClient`/`OpenAiRestClient`
injetados na própria factory), porque as base URLs deles são fixas — só
a apiKey da OpenAI muda por chamada, e isso é só um header
(`Authorization: Bearer ...`), não precisa de client dinâmico.
`ProvedorNaoConfiguradoLlmProvider` (null object) cobre `ProvedorIa.NENHUM`
— lança `IaNaoConfiguradaException` se algo tentar chamar `chat()`/
`embed()` sem provedor configurado (422 na spec).

Simplificação consciente, registrada pra não esquecer: `ConfiguracaoIa.ollamaUrl`
(URL de Ollama customizada por usuário — já aceita pela spec) ainda não
é usada de verdade pelo `OllamaLlmProvider`, que sempre aponta pra
instância default do `application.properties`. Dar suporte a URL
customizada por usuário exigiria construir o REST client
dinamicamente por chamada (`RestClientBuilder` programático em vez do
client CDI fixo) — decidido não adiantar essa complexidade sem
necessidade real ainda.

6 testes novos: `LlmProviderFactoryImplTest` (a lógica de dispatch por
`ProvedorIa` É testada, diferente dos adapters — não é "thin HTTP
adapter", é decisão real que pode ter bug) e
`ProvedorNaoConfiguradoLlmProviderTest`. Sem teste dedicado pros dois
adapters (mesmo critério já estabelecido no projeto desde o
`OllamaLlmProvider` do document-service — thin adapter, mockado nos
testes de quem usa a porta, nunca testado contra o provedor real em
CI). 30 testes no total do `ai-service`, `./mvnw test` verde.

Criado: `services/ai-service/src/main/java/.../domain/` (`ChatRequest`,
`ChatResponse`, `EmbeddingResult`, `LlmProvider`, `LlmProviderFactory`,
`IaNaoConfiguradaException`), `.../infrastructure/llm/` (`OllamaRestClient`/
`OllamaLlmProvider`, `OpenAiRestClient`/`OpenAiLlmProvider`,
`ProvedorNaoConfiguradoLlmProvider`, `LlmProviderFactoryImpl`, `dto/`
com os DTOs do Ollama e da OpenAI) + testes. Alterado:
`services/ai-service/src/main/resources/application.properties`
(config de Ollama/OpenAI), `docs/tasks.md` (item 5 concluído).

## 2026-08-10 — item 6: clientes de saída do ai-service

Pedido: "pode seguir pro item 6".

Três portas (`BudgetServiceClient`, `CardServiceClient`,
`TransactionServiceClient`) + REST clients, cobrindo quatro das cinco
tools MCP documentadas em `ai-strategy.md` (a quinta, `buscar_transacoes`
com busca semântica, fica pro item 7 — RAG/Qdrant).

Achado ao implementar `buscar_saldo_disponivel`: a tabela de
`ai-strategy.md` (escrita em 2026-08-06, antes do `budget-service`
existir) dizia que essa tool chama "budget-service + account-service".
Na prática, `GET /disponivel-para-gastar` do `budget-service` **já**
devolve o valor pronto (ele mesmo agrega saldo de conta internamente,
ADR-0026) — não tem motivo pro `ai-service` chamar `account-service` de
novo pra um dado que o `budget-service` já buscou. Resultado: **nenhum**
`AccountServiceClient` neste serviço, ajuste registrado explicitamente
em `docs/tasks.md` pra não parecer omissão.

Mesmo padrão de simplicidade do `budget-service` (item 4 daquela
fatia): um `PropagarAutorizacaoHeadersFactory` só, compartilhado pelos
três REST clients — nenhum dos cinco endpoints chamados
(`GET /disponivel-para-gastar`, `GET /cartoes`,
`GET /cartoes/{id}/faturas`, `GET /transacoes`,
`GET /transacoes/resumo-por-categoria`, `POST /transacoes`,
`POST /transacoes-recorrentes`) exige confirmação de posse — todos já
filtram pelo `sub` do token propagado.

`TransactionServiceClient.criarTransacao`/`criarTransacaoRecorrente` são
os dois únicos métodos de escrita de toda a fatia até agora — existem
só como capacidade do cliente; a responsabilidade de só chamá-los depois
de confirmação explícita do usuário (ADR-0007) fica pro agente
orquestrador (item 8), não faz parte deste item.

Sem teste dedicado pros três `*ClientImpl` — mesmo critério já
estabelecido no projeto (thin adapter HTTP, mockado nos testes de quem
usa a porta). `./mvnw compile`/`test` validados, 30 testes continuam
verdes.

Criado: `services/ai-service/src/main/java/.../domain/`
(`DisponivelParaGastar`, `BudgetServiceClient`, `StatusFatura`,
`Cartao`, `Fatura`, `CardServiceClient`, `ResumoCategoria`, `Transacao`,
`CriarTransacaoComando`, `CriarTransacaoRecorrenteComando`,
`TransactionServiceClient`), `.../infrastructure/client/`
(`PropagarAutorizacaoHeadersFactory`, `BudgetServiceRestClient`/
`ClientImpl`, `CardServiceRestClient`/`ClientImpl`,
`TransactionServiceRestClient`/`ClientImpl`, `dto/`). Alterado:
`services/ai-service/src/main/resources/application.properties`
(REST clients de budget/card/transaction-service), `docs/tasks.md`
(item 6 concluído).

## 2026-08-10 — item 7: RAG (Qdrant) do ai-service

Pedido: "pode seguir pro item 7".

Achado real logo no início, fora do escopo original do item: o payload
de `TransacaoRegistradaEvento` (publicado em `transacao.eventos` pelo
`transaction-service` desde a fatia 1) não carregava `descricao` nem
`categoria` — só id/contaId/usuarioId/tipo/valor/data. RAG é
especificamente sobre indexar *descrição* de transação
(ai-strategy.md seção 2, "'gastos com mercado' deve encontrar transações
com descrição 'Supermercado Pão de Açúcar'") — sem o campo, não tinha o
que indexar. Adicionados os dois campos ao evento e ao publisher.
Confirmado antes de mexer que nenhum consumidor desse tópico existia
ainda (só o `transaction-service` publicava, ninguém ouvia) — mudança
puramente aditiva, sem consumidor pra quebrar; suite de 102 testes do
`transaction-service` continuou verde depois.

Decisão de design nova ao encaixar RAG na arquitetura de múltiplos
provedores (ADR-0002): **embedding sempre usa Ollama local**,
independente do provedor de *chat* que o usuário escolheu.
`LlmProviderFactory` ganhou um método novo, `criarParaEmbedding()`,
sempre devolvendo Ollama. Motivo concreto: `nomic-embed-text` (Ollama)
gera vetor de 768 dimensões, `text-embedding-3-small` (OpenAI) gera
1536 — uma coleção do Qdrant tem dimensão fixa, então indexar com o
provedor de cada usuário quebraria a coleção na primeira busca cruzando
usuários com provedores diferentes. Efeito colateral bom: indexação não
depende de `ConfiguracaoIa` — toda transação é indexada assim que é
criada, mesmo pra quem nunca configurou IA ainda (dado já fica pronto
pra quando configurar).

Pipeline completo: `transaction-service` publica → `TransacaoRegistradaConsumer`
(primeiro consumer real desse tópico) → `IndexarTransacaoUseCase` (embed
+ indexa no Qdrant). `BuscarTransacoesSimilaresUseCase` é o lado de
consulta, usado pela tool `buscar_transacoes` no item 8 — sempre filtra
por `usuarioId` na busca (isolamento multi-tenant, ADR-0003, mesma regra
de todo o resto do sistema).

`QdrantVectorStoreImpl` fala REST puro com o Qdrant — decisão
consciente de não usar um client Java dedicado (não verificado/confiável
sem acesso à internet pra conferir Maven coordinates certas; REST puro é
o mesmo padrão já usado com Ollama/OpenAI/os quatro outros serviços, sem
introduzir dependência nova arriscada). Contrato da API (criar coleção
`PUT /collections/{nome}`, upsert `PUT .../points`, busca
`POST .../points/search` com filtro por `usuarioId`, 404 de coleção
inexistente) validado na prática com `curl` direto contra o container
Qdrant já rodando desde o item 3 — os três fluxos (criar, indexar,
buscar) responderam exatamente como os DTOs esperavam, sem surpresa.
`QdrantColecaoInicializador` cria a coleção sozinho no startup se não
existir (idempotente).

9 testes novos: `IndexarTransacaoUseCaseTest`,
`BuscarTransacoesSimilaresUseCaseTest`, `TransacaoRegistradaConsumerTest`
(mapeamento evento→comando, mesmo padrão do
`DocumentoLancamentosConfirmadosConsumerTest` já usado no
transaction-service), `LlmProviderFactoryImplTest` ganhou um caso pro
`criarParaEmbedding()`. Sem teste dedicado pro `QdrantVectorStoreImpl` —
thin HTTP adapter, validado hoje via `curl` real em vez de mock, mesmo
critério já estabelecido no projeto pra esse tipo de classe. 34 testes
no total do `ai-service`, `./mvnw test` verde.

Criado: `services/ai-service/src/main/java/.../domain/`
(`RegistroIndexado`, `ResultadoBusca`, `VectorStore`),
`.../application/` (`IndexarTransacaoComando`/`UseCase`,
`BuscarTransacoesSimilaresUseCase`), `.../infrastructure/messaging/`
(`TransacaoRegistradaEvento`, `TransacaoRegistradaDeserializer`,
`TransacaoRegistradaConsumer`), `.../infrastructure/vectorstore/`
(`QdrantRestClient`, `QdrantVectorStoreImpl`,
`QdrantColecaoInicializador`, `dto/`) + testes. Alterado:
`services/transaction-service/.../TransacaoRegistradaEvento.java`
(campos `descricao`/`categoria`),
`services/transaction-service/.../TransacaoEventPublisherImpl.java`,
`services/ai-service/pom.xml` (Kafka), `application.properties` (Kafka +
Qdrant), `docs/tasks.md` (item 7 concluído).

## 2026-08-10 — item 8: agente orquestrador do ai-service

Pedido: "pode seguir pro item 8" — o item mais complexo da fatia.

Achado real logo no início: `AcaoPendente` (item 4) não tinha campo
`descricao`, mas `criar_transacao` do transaction-service exige esse
campo — sem ele a ação confirmada não tinha o que executar. Adicionado
`AcaoPendente.descricao` (domínio + persistência + os testes do item 4
já existentes, todos revalidados depois da mudança).

`AgenteOrquestradorUseCase` — um caso de uso só pra tudo que acontece na
conversa (pergunta, ação, correção, confirmação), consistente com o
`POST /chat` único já decidido na spec (item 1). Decisões de design
tomadas ao montar o fluxo, nenhuma prevista em ADR anterior:

- Confirmação detectada por palavra-chave (conjunto fixo de frases,
  case-insensitive), não por outra chamada ao LLM — mais simples e sem
  mais um ponto de falha de JSON malformado pra uma decisão binária.
- Correção tratada como comando novo (limpa a proposta antiga,
  reclassifica a mensagem do zero) em vez de um "diff" com estado da
  proposta anterior — mesmo resultado observável que `ai-strategy.md`
  seção 4.2 descreve, com bem menos complexidade de estado.
- `descricao` sempre vem do LLM; `contaId` nunca — é sempre resolvido em
  código Java determinístico contra `AccountServiceClient` (match por
  substring), nunca um UUID inventado pelo modelo. Isso trouxe de volta
  `AccountServiceClient` pro `ai-service` (tinha sido descartado no item
  6, que só cobria as tools de leitura — a necessidade real só apareceu
  aqui, no fluxo de escrita, pra resolver "conta corrente" → UUID).
- A resposta final de toda consulta é montada em template Java, nunca
  pelo LLM — o número exato vem de chamada determinística a um serviço
  e é só formatado; o LLM decide qual tool chamar e quais parâmetros
  extrair, nunca escreve o texto com o número final (PRD: "nunca
  inventada").

Bug real encontrado escrevendo o teste de "ação expirada", corrigido
antes de qualquer commit: a condição original só entrava no fluxo de
confirmação quando `temAcaoPendenteValida()` — isso excluía justamente
uma proposta **expirada**, que caía num "não entendi" genérico em vez de
avisar que expirou. Corrigido pra checar só a presença da ação
(`getAcaoPendente() != null`), deixando `Conversa.confirmarAcaoPendente`
decidir validade — o catch de `AcaoPendenteExpiradaException` (que
existia desde o item 4) virou alcançável de verdade só agora.

**Prompts testados contra o Ollama real via `curl`** — mesmo cuidado que
o `AgenteExtracaoFaturaService` do document-service teve (nunca confiar
só na teoria pra prompt de LLM, `docs/historico.md` 2026-08-09):
- Classificação de intent acertou de primeira: pergunta de saldo →
  `{"intent":"CONSULTA","tool":"buscar_saldo_disponivel","periodo":"MES_ATUAL"}`.
- Extração de ação, na primeira versão do prompt, confundiu `descricao`
  com o texto da conta — comando "...na conta corrente" virou
  `descricao: "Conta Corrente"` em vez de algo sobre a despesa em si.
  Prompt corrigido com instrução explícita ("NUNCA repita esse valor em
  descricao") e revalidado contra o Ollama de novo: passou a extrair
  "Aluguel" — um palpite plausível pro contexto (o comando de teste não
  mencionava nenhuma descrição real), aceitável porque é exatamente pra
  isso que existe a confirmação (ADR-0007) — o usuário vê o resumo
  completo antes de qualquer coisa persistir, e pode recusar/corrigir se
  o palpite estiver errado.
- Pergunta comparativa ("gastei mais que o mês passado?") fez o modelo
  devolver um período inválido, dois valores concatenados numa string só
  (`"MES_ATUAL,MES_PASSADO"`) — o parsing defensivo já tinha fallback
  pra isso (`PeriodoReferencia` desconhecido → `MES_ATUAL`), não quebrou
  nada. Aproveitado o achado pra melhorar de verdade
  `responderResumoCategoria`: passou a somar `totalGastoPeriodoAnterior`
  (campo que o `transaction-service` já calculava por categoria, mas a
  resposta não usava) e responder "maior"/"menor" que o período anterior
  diretamente — sem pedir nenhuma aritmética ao LLM, mesma lição já
  aprendida no `document-service`.

16 testes novos em `AgenteOrquestradorUseCaseTest`: as quatro tools de
consulta (incluindo o teste novo de comparação com período anterior),
proposta de ação com conta resolvida, pedido de conta quando não
identificada, extração incompleta (pede reformulação), intent
desconhecida, confirmação de ação pontual e recorrente, proposta
expirada (com a correção do bug acima), correção limpando a proposta
anterior antes de reclassificar, e isolamento de conversa por usuário
(mesmo erro nos dois casos — id inexistente ou de outro usuário — evita
IDOR). 51 testes no total do `ai-service`, `./mvnw test` verde.

Criado: `services/ai-service/src/main/java/.../domain/` (`Conta`,
`AccountServiceClient`, `Intencao`, `ToolConsulta`, `PeriodoReferencia`),
`.../application/` (`ChatComando`, `RegistroTrace`, `ChatResultado`,
`AgenteOrquestradorUseCase`), `.../infrastructure/client/`
(`AccountServiceRestClient`/`ClientImpl`, `dto/ContaDto`),
`.../infrastructure/agent/dto/` (`IntencaoDetectadaDto`,
`AcaoExtraidaDto`) + testes. Alterado:
`services/ai-service/src/main/java/.../domain/AcaoPendente.java`
(campo `descricao`), `.../infrastructure/persistence/AcaoPendenteEmbedded.java`,
`.../infrastructure/persistence/ConversaMapper.java`,
`src/test/.../domain/AcaoPendenteTest.java`,
`src/test/.../domain/ConversaTest.java` (assinatura nova de
`AcaoPendente.propor`), `application.properties` (REST client
account-service), `docs/tasks.md` (item 8 concluído).

## 2026-08-10 — item 9: REST do ai-service (fecha a implementação)

Pedido: "pode seguir pro item 9".

Três resources em `infrastructure/rest/`: `ChatResource`
(`POST /chat`), `ConversaResource` (`GET /conversas`,
`GET /conversas/{id}`), `ConfiguracaoResource`
(`GET/PUT /configuracao`) — mesmo molde do resto do projeto
(`usuarioIdAutenticado()` extrai `sub` do JWT, DTOs de resposta com
`static de(...)`). Spec ganhou o campo `descricao` em `AcaoProposta`,
que não existia quando a spec foi escrita no item 1 — só apareceu
quando o domínio ganhou esse campo no item 8.

Achado de nomenclatura, não um problema de verdade: os DTOs REST
`ChatRequest`/`ChatResponse` (o que o cliente web/mobile manda/recebe)
têm o mesmo nome dos `ChatRequest`/`ChatResponse` do domínio (o que o
`LlmProvider` usa internamente pra falar com Ollama/OpenAI) — sem
colisão real porque vivem em pacotes diferentes
(`infrastructure.rest.dto` vs `domain`) e nenhum arquivo precisa
importar os dois ao mesmo tempo.

Quatro casos de uso novos, pequenos, seguindo o padrão já estabelecido
desde a fatia 2 (`BuscarCartaoUseCase` pro anti-IDOR,
`DefinirReservaUseCase` pro upsert): `ListarConversasUseCase`,
`BuscarConversaUseCase`, `BuscarConfiguracaoIaUseCase`,
`DefinirConfiguracaoIaUseCase`.

Achado real rodando o primeiro teste de integração completo do
`ai-service` (`@QuarkusTest` sobe a aplicação inteira, incluindo o
`QdrantColecaoInicializador` do item 7, que até agora só tinha sido
exercido por testes unitários com mock): o REST client do Quarkus lança
`org.jboss.resteasy.reactive.ClientWebApplicationException` pra
qualquer erro HTTP, **não** `jakarta.ws.rs.NotFoundException`
especificamente — o `catch (NotFoundException e)` escrito no item 7
nunca teria disparado de verdade contra o Qdrant real, só não tinha
sido pego antes porque nenhum teste subia a aplicação completa até
agora. Corrigido pra `catch (WebApplicationException e)` + checar
`e.getResponse().getStatus() == 404` manualmente — mais robusto, não
depende de qual subtipo exato de exceção o client decide lançar pra
cada status.

Exception mapper novo: `IllegalArgumentExceptionMapper` (400) — cobre
validação de campo cruzado que o Bean Validation da request não
expressa sozinho (`apiKey` obrigatória só se `provedor=OPENAI`,
regra que já vivia em `ConfiguracaoIa.validarProvedor` desde o item 4,
mas sem esse mapper viraria 500 em vez de 400).

13 testes de integração novos: `ChatResourceTest` (consulta de ponta a
ponta com todos os clientes mockados via `QuarkusMock` — mesmo padrão
do `CartaoResourceTest` do card-service —, 400/404/401),
`ConversaResourceTest` (sem endpoint de criação direta, a conversa de
teste é criada via `POST /chat` de verdade antes de listar/buscar;
isolamento por usuário confirmado), `ConfiguracaoResourceTest`
(confirma que `apiKey` nunca aparece na resposta, mesmo depois de
definida). 64 testes no total do `ai-service` — **fecha a
implementação da fatia 5** (só falta o item 10:
docker-compose/CI/Keycloak/Postman/diagramas).

Criado: `services/ai-service/src/main/java/.../application/`
(`ListarConversasUseCase`, `BuscarConversaUseCase`,
`BuscarConfiguracaoIaUseCase`, `DefinirConfiguracaoIaComando`/`UseCase`),
`.../infrastructure/rest/` (`ChatResource`, `ConversaResource`,
`ConfiguracaoResource`, `ConversaNaoEncontradaExceptionMapper`,
`IaNaoConfiguradaExceptionMapper`, `ConstraintViolationExceptionMapper`,
`IllegalArgumentExceptionMapper`, `dto/` — `ChatRequest`, `ChatResponse`,
`AcaoProposta`, `ToolInvocada`, `ConversaResumo`,
`ConversaDetalheResponse`, `MensagemConversa`, `ConfiguracaoIaRequest`,
`ConfiguracaoIaResponse`, `ErroResponse`, `ErroValidacaoResponse`) +
testes de integração. Alterado:
`services/ai-service/.../infrastructure/vectorstore/QdrantColecaoInicializador.java`
(catch corrigido), `docs/specs/ai-service.yaml` (campo `descricao` em
`AcaoProposta`), `docs/tasks.md` (item 9 concluído).

## 2026-08-10 — item 10: fecha a fatia 5 (docker-compose, CI, Keycloak, Postman, diagramas)

Pedido: "pode seguir pro item 10" — mesmo padrão de fechamento já usado no
item 11 da fatia 3 (`document-service`) e no item 7 da fatia 4
(`budget-service`).

`docker-compose.yml` ganhou o serviço `ai-service` (depende de mongo/
qdrant/kafka/ollama/account-service/budget-service/card-service/
transaction-service, porta 8086). Diferente das fatias anteriores, o
`Dockerfile.jvm` precisa do `target/quarkus-app/` já compilado — rodei
`./mvnw package -B -DskipTests` no `ai-service` antes do
`docker compose up -d --build ai-service` (o build direto do compose
falhou primeiro, "target/quarkus-app/lib: not found", até eu perceber
que faltava esse passo — não é diferente de como os outros serviços já
funcionam, só não tinha ficado óbvio até agora porque os builds
anteriores desta sessão sempre reaproveitaram uma imagem já buildada).
Subida validada de verdade: build limpo, `GET /q/health` → 200 (Mongo,
Kafka consumer de `transacao.eventos` e o próprio `ai-service` todos
UP), log confirmou `QdrantColecaoInicializador` reconhecendo a coleção
"transacoes" já existente em vez de tentar recriar — validação real do
fix de item 9 (catch de `WebApplicationException`) contra o Qdrant de
verdade, não só via `@QuarkusTest`.

Keycloak: client `ai-service` registrado com `bearerOnly: true`, **sem**
service-account — mesmo motivo do `budget-service`/`document-service`:
`ai-service` só propaga o token do próprio usuário pros serviços que
chama, nunca client_credentials. Aplicado no realm já importado no
container rodando (`kcadm.sh create clients`) e testado de ponta a
ponta: token real emitido pro `usuario.teste` via `password grant`,
`GET /api/v1/configuracao` e `GET /api/v1/conversas` retornaram 200
contra o container recém-subido (sem token, 401 — confirmando que o
`RolesAllowed("usuario")` está mesmo ativo).

CI: job `ai-service` novo em `ci.yml`, mesmo template dos outros cinco
(testes + cobertura JaCoCo + build de imagem Docker de validação + OWASP
Dependency-Check), `ai-service` adicionado ao filtro de paths do job
`changes`.

Postman: os 5 endpoints do `ai-service` documentados em
`mudancas-manuais.txt` (`POST /chat` com um exemplo de body pra cada
fluxo — consulta, propor ação, confirmar, corrigir —, `GET /conversas`,
`GET /conversas/{id}`, `GET/PUT /configuracao` com exemplo pra Ollama e
OpenAI) + `ai_service_url` no environment.

Documentação viva atualizada pra refletir a fatia entregue: `diagrams.md`
(container graph sem mais "(planejado)" no `ai-service` nem "proposto"
no Qdrant; arestas síncronas novas pros quatro serviços consultados +
aresta Kafka nova de `transacao.eventos` pro `ai-service`, indexação
RAG; seção 3.6 nova com o ER conceitual de `Conversa`/`Mensagem`/
`AcaoPendente`/`ConfiguracaoIa` — `Mensagem`/`AcaoPendente` embutidos,
não coleções próprias; seção 4.7 nova com o diagrama de classes do
domínio, incluindo os ports `LlmProvider`/`LlmProviderFactory`/
`VectorStore`; índice de fluxos da seção 6 atualizado); `overview.md`
(tabela de serviços → ✅ Entregue; seção 4 reescrita pra bater com a
implementação real — endpoint único decidindo intenção por texto+estado,
confirmação por casamento de palavra-chave em vez de outra chamada ao
LLM, resposta de consulta sempre montada por template Java — e subseção
4.3 nova com o fluxo de indexação RAG via Kafka); `roadmap.md` (fatia 5
→ ✅ Entregue, próxima ação aponta pra fatia 6, front-end); `README.md`
raiz (parágrafo do `ai-service` no "Estado atual", tabela de endpoints,
URLs — REST/Swagger/MongoDB compartilhado com document-service —,
contagem de serviços no CI/docker compose de cinco pra seis, e o bullet
de stack sobre IA ajustado: tirei a menção a "MCP" porque a
implementação real do agente orquestrador usa tools internas resolvidas
em Java, não expõe de fato o protocolo MCP — `ai-strategy.md` descreve
isso como visão, não foi revisitado nesta fatia, então o README não
devia prometer mais do que existe).

Isso fecha a fatia 5 do roadmap — `ai-service` entregue de ponta a
ponta: spec (item 1) → ADR-0005 confirmado → scaffold (item 2) → Qdrant
(item 3) → domínio/persistência (item 4) → `LlmProvider`/adapters (item
5) → clientes de saída (item 6) → RAG (item 7) → agente orquestrador
(item 8) → REST (item 9) → infra/CI/documentação (item 10). 64 testes,
CI configurado, container validado contra a stack real (Mongo, Kafka,
Qdrant, Keycloak — todos de verdade, não mock).

Alterado: `docker-compose.yml`, `infra/keycloak/realm-financas.json`,
`.github/workflows/ci.yml`, `docs/postman/mudancas-manuais.txt`,
`docs/postman/financas-dev.postman_environment.json`,
`docs/architecture/diagrams.md`, `docs/architecture/overview.md`,
`docs/roadmap.md`, `README.md`, `docs/tasks.md` (item 10, fatia 5
fechada).

## 2026-08-10 — três correções pós-push do ai-service (CI real pegou o que a validação local não pegou)

Pedido: "sim, pode fazer o commit" seguido de "sim, pode fazer push" — o
CI do primeiro push falhou três vezes seguidas, cada uma revelando algo
que só o ambiente real do runner expõe (mesmo padrão de "achado real"
já visto nas fatias anteriores, dessa vez em série):

1. **`mvnw` sem permissão de execução.** `git core.filemode=false` no
   Windows fez o `mvnw` do `ai-service` entrar no commit como `100644`
   em vez de `100755` (os outros cinco serviços têm `100755`) — job
   falhou com "exit code 126" (permissão negada) antes mesmo de rodar
   um teste. Corrigido com `git update-index --chmod=+x mvnw` (o `chmod
   +x` sozinho não basta no Windows, o filesystem não tem bit de
   execução real — precisa forçar via update-index).

2. **`QdrantColecaoInicializador` derrubava a subida inteira do
   Quarkus se o Qdrant estivesse fora do ar.** Corrigido o `mvnw`, o CI
   rodou de verdade e travou nos testes: `Failed to start quarkus`,
   causa raiz `Connection refused: localhost:6333`. Diferente de
   MySQL/Kafka/MongoDB (que o Quarkus sobe sozinho via Dev Services/
   Testcontainers quando não há config explícita de conexão em teste),
   não existe Dev Service pra um REST client puro como o Qdrant — o
   runner de CI simplesmente não tem Qdrant disponível. A suíte só
   passava localmente porque o Qdrant do `docker-compose` já estava de
   pé na máquina de dev, mascarando o problema. `QdrantColecaoInicializador`
   só tratava erro HTTP (`WebApplicationException`, ex: 404 pra criar a
   coleção) — falha de conexão subia como `RuntimeException` não
   tratada dentro do `@Observes StartupEvent`, derrubando a aplicação
   inteira. Corrigido pra tratar essa falha como warning (RAG fica
   indisponível até o Qdrant voltar, mas chat/configuração continuam
   funcionando) — validado localmente parando o container Qdrant e
   rodando a suíte de propósito: 64/64 passam mesmo sem Qdrant no ar.

3. **Falso positivo de CVE do `quarkus-mongodb-*`, mesmo problema já
   resolvido no `document-service`.** Terceiro push corrigiu os testes,
   CI avançou até o scan de vulnerabilidade e falhou com o EXATO mesmo
   CPE mal mapeado já diagnosticado e documentado pro `document-service`
   (`quarkus-mongodb-panache`/`-client` confundidos com "MongoDB
   Server", CVE-2021-32036/2025-14847/2026-9753/2014-8180) — só não
   tinha sido replicado pro `ai-service` porque o item 10 nunca rodou o
   dependency-check localmente (falta de `NVD_API_KEY` fora do CI, mesmo
   motivo de sempre). Corrigido copiando o mesmo
   `dependency-check-suppression.xml` do `document-service` (adaptado)
   + `<suppressionFiles>` no `pom.xml`.

CI verde depois dos três fixes. Lição prática pra próxima fatia com
Mongo (não tem mais nenhuma planejada, mas vale registrar): sempre
copiar o `dependency-check-suppression.xml` de um serviço Mongo
existente **no mesmo item que adiciona `quarkus-mongodb-panache` ao
pom.xml**, não esperar o dependency-check da fatia nova falhar pra
lembrar — e todo `mvnw` novo (scaffold copiado no Windows) precisa de
`git update-index --chmod=+x mvnw` explícito antes do primeiro commit,
`chmod +x` sozinho não é suficiente nesse ambiente.

Alterado: `services/ai-service/mvnw` (modo 100644→100755),
`services/ai-service/src/main/java/.../vectorstore/QdrantColecaoInicializador.java`
(catch de `RuntimeException` genérico, não derruba mais a subida),
`services/ai-service/pom.xml` (`<suppressionFiles>`). Criado:
`services/ai-service/dependency-check-suppression.xml`.

## 2026-08-10 — fatia 6: decisões de front-end e item 1 (scaffold do Next.js)

Pedido: "pode começar a fatia 6, front-end Next.js".

ADR-0006 tinha deixado três decisões explicitamente em aberto pra quando
essa fatia começasse (estilo/componentes, autenticação, identidade
visual) — perguntadas ao usuário antes de qualquer código:

- **Estilo/componentes**: Tailwind CSS + shadcn/ui (Recomendado,
  escolhido). shadcn/ui não é dependência de runtime — os componentes
  são copiados pro repo (`components/ui/`), zero lock-in.
- **Autenticação**: Auth.js/NextAuth v5 com provider OIDC genérico
  contra o Keycloak (Recomendado, escolhido) — formalizado em
  **ADR-0027** (sessão JWT em cookie httpOnly, access token nunca
  exposto ao client-side, refresh automático, logout também encerra a
  sessão SSO no Keycloak, reaproveita o client `web-app` já existente).
- **Direção visual inicial**: paleta neutra/profissional — cinza + azul
  de destaque (Recomendado, escolhido) — ajustável depois.

Aproveitei pra corrigir uma lacuna: ADR-0026 (regra de cálculo do
budget-service) nunca tinha entrado no índice de `docs/architecture/adr/README.md`
— corrigido junto com a entrada do ADR-0027 novo.

Backlog completo da fatia 6 escrito em `docs/tasks.md` (9 itens: scaffold →
autenticação → layout base → CRUD conta → CRUD transação → dashboard →
upload de documento → chat com IA → fechamento), espelhando o nível de
detalhe já usado nas fatias 4/5. `roadmap.md` atualizado (fatia 6 → 🔶 Em
andamento).

**Item 1 — scaffold do projeto**, executado na sequência:

`services/web/` criado com `create-next-app` (Next.js 16.3.0, App
Router, Turbopack, TypeScript, Tailwind CSS v4, `--no-src-dir`). Next.js
16 tem mudanças de convenção reais em relação ao que já era conhecido —
o próprio pacote injeta um `AGENTS.md`/`CLAUDE.md` avisando pra consultar
`node_modules/next/dist/docs/` antes de escrever código; confirmado ali
que `proxy.ts` substituiu `middleware.ts` como convenção de arquivo (via
`app-guides/authentication.md` e `project-structure.md`) — relevante pro
item 2 (autenticação), guardado pra lá.

`shadcn/ui` inicializado com `baseColor: neutral` (já bate com a decisão
de paleta). Tokens de cor em `app/globals.css` editados na mão pra trocar
`--primary`/`--ring`/`--sidebar-primary` de cinza puro pra azul
(`oklch(0.546 0.185 259.8)` claro / `oklch(0.65 0.16 259.8)` escuro,
foreground branco/preto conforme o tema) — resto da paleta (secondary,
muted, accent, destructive, border) continua neutro/cinza, só a cor de
ação/foco principal ganhou destaque. `next.config.ts` configurado com
`output: "standalone"` (necessário pro Dockerfile multi-stage). Página
inicial (`app/page.tsx`) e metadata (`app/layout.tsx`, `lang="pt-BR"`)
trocadas do template de marketing padrão do `create-next-app` por um
placeholder mínimo — SVGs de exemplo (Next.js/Vercel logo) removidos de
`public/`.

Tooling de teste instalado seguindo exatamente o que
`testing-strategy.md` seção 3 já tinha decidido antes desta fatia:
Vitest + React Testing Library + MSW (`jsdom`, `@testing-library/jest-dom`).
Configuração seguiu o guia oficial embutido no pacote (`node_modules/next/dist/docs/.../testing/vitest.md`)
— `vitest.config.mts` (extensão `.mts` de propósito, evita o aviso de
CJS/ESM que apareceria com `.ts`) usa `resolve.tsconfigPaths: true`
nativo do Vite em vez do plugin `vite-tsconfig-paths` (que o próprio
Vite já sinaliza como redundante agora). Smoke test
(`__tests__/page.test.tsx`) confirma que a página inicial renderiza —
primeiro teste da fatia, mais virão em cada item que adicionar tela real.

**Achado real ao instalar as dependências de teste**: `npx shadcn@latest
init` colocou o próprio pacote `shadcn` (a CLI) em `dependencies` no
`package.json` gerado, em vez de `devDependencies` — nunca é importado
em runtime, só usado via `npx shadcn add <componente>` durante
desenvolvimento, então movido na mão. Isso expôs um conflito de peer
dependency ao instalar `@vitejs/plugin-react`: o `shadcn` depende de
Babel 7 (`@babel/core@^7`), e uma dependência opcional do Vite/rolldown
(`@rolldown/plugin-babel`, peer de `@vitejs/plugin-react`) pede Babel 8
— `npm install` recusou resolver sozinho. Resolvido com
`--legacy-peer-deps`, seguro aqui porque o conflito é inteiramente entre
ferramentas de desenvolvimento (nenhuma das duas cadeias de Babel entra
no bundle de produção).

`Dockerfile` multi-stage escrito (builder `node:24-alpine` + `npm ci` +
`npm run build`; runner só copia `.next/standalone` + `.next/static` +
`public/`, roda como usuário não-root `nextjs`) — validado de ponta a
ponta: `npm run lint`, `npm run test` e `npm run build` limpos,
`docker build -t web:ci .` + `docker run` respondendo `200` em `GET /`.
`.dockerignore` e `.gitignore` do `services/web/` ajustados pra permitir
commitar um futuro `.env.example` (o gerado por padrão pelo
`create-next-app` ignora `.env*` inteiro, sem a exceção que o
`.gitignore` raiz já tem).

Criado: `services/web/` (projeto Next.js completo — scaffold, shadcn/ui,
tooling de teste, `Dockerfile`, `README.md`), `docs/architecture/adr/0027-autenticacao-frontend-authjs-keycloak.md`.
Alterado: `docs/architecture/adr/README.md` (índice — ADR-0026 que
faltava + ADR-0027 novo), `docs/tasks.md` (backlog da fatia 6 + item 1
concluído), `docs/roadmap.md` (fatia 6 → 🔶 Em andamento).

## 2026-08-10 — feedback: deixar front-end acessível pro usuário conferir no navegador

Pedido explícito: "toda vez que uma implementação, refatoração ou algo
que modifique uma pagina, eu quero poder acessar pelo meu navegador e
verificar como esta". Salvo como memória de feedback — a partir de
agora, toda mudança de página/UI termina com `npm run dev` rodando e a
URL relevante informada, não só validação minha via lint/teste/build.
Já apliquei subindo o dev server do scaffold do item 1
(`http://localhost:3000`) pro usuário conferir antes de seguir.

## 2026-08-10 — item 2: autenticação (Auth.js + Keycloak, ADR-0027)

Pedido: "podemos ir para o proximo passo" (depois de conferir o item 1
no navegador).

`next-auth@5.0.0-beta.32` instalado (`peerDependencies` confirma
suporte a `next@^16.0.0` antes de instalar — não é garantido por padrão
numa versão tão nova do Next.js). Provider nativo
`next-auth/providers/keycloak` (não o OIDC genérico que o ADR-0027
tinha cogitado — o Keycloak já tem provider dedicado no Auth.js, mais
direto).

**Decisão tomada lendo o código-fonte real do `@auth/core`, não a
documentação superficial**: o client `web-app` do Keycloak é público
(sem `client_secret`, `publicClient: true`) — usado também pelo Postman
pra password grant (`docs/postman/README.md` seção 3), então mudar pra
confidential quebraria esse fluxo já estabelecido. O provider nativo do
Keycloak no Auth.js vem com exemplo de configuração usando
`clientSecret` (client confidential é o caminho "batido"), mas ler
`@auth/core/lib/actions/callback/oauth/callback.js` confirmou que
`client: { token_endpoint_auth_method: "none" }` no provider é
suficiente pra funcionar sem secret — o Auth.js já usa PKCE + state por
padrão pra qualquer provider OIDC (`checks: c.checks ?? ["pkce"]` em
`providers.js`), então authorization code sem client secret continua
seguro (RFC 7636). Sem esse `token_endpoint_auth_method: "none"`
explícito, o Auth.js tentaria enviar um header Basic Auth com
`client_secret` `undefined` e o Keycloak rejeitaria a troca de token.

**Achado crítico, também só visível lendo o código-fonte**: sem adapter
de banco (nosso caso — sessão é só JWT, ADR-0027), o Auth.js **gera um
`user.id` aleatório a cada login** (`crypto.randomUUID()` em
`getUserAndAccount`, dentro de `callback.js`) — não usa o `sub` do
id_token por padrão, porque esse design pressupõe normalmente um
adapter de banco que teria seu próprio PK de usuário independente do
provider. Se eu não tivesse notado isso e sobrescrito no `callbacks.jwt`
(`token.sub = profile.sub`, usando o `profile` cru — não o `user`
processado — que o próprio Auth.js passa pro callback só no login
inicial), `session.user.id` nunca bateria com o `usuarioId` usado em
TODOS os outros serviços do sistema (ADR-0003) — um bug de
multi-tenancy silencioso, do tipo que só aparece em produção quando
alguém tenta cruzar dado do front-end com o back-end e nada bate.
Validei isso de propósito simulando o login completo via `curl`
(GET da tela de login do Keycloak, POST de usuário/senha, follow do
redirect de callback OAuth) contra a stack real: `session.user.id`
saiu `cd4cf57c-b5a8-4b2c-b9b5-ffba5770e19d` — exatamente o mesmo `sub`
que `usuario.teste` já tinha em todo teste anterior deste projeto
(confirmado comparando com token JWT decodificado em sessões
anteriores). Sem essa validação de ponta a ponta contra o Keycloak de
verdade (não só `@QuarkusTest`-equivalente ou mock), esse bug
específico não teria aparecido nem em teste unitário nem em build —
só numa integração real.

Design de onde cada campo do token vive, e por quê: `callbacks.jwt`
guarda `accessToken`/`refreshToken`/`idToken`/`expiresAt` no cookie
JWT httpOnly (nunca no lado do cliente). `callbacks.session` — o que
literalmente vira o corpo JSON de `GET /api/auth/session`, inclusive
quando chamado a partir de `auth()` em Server Component/Route Handler,
já que é o mesmo pipeline — **nunca** inclui token nenhum, só
`user.id`/`name`/`email`/`error`; isso é deliberado (ADR-0027: "nunca
exposto ao client-side"), não uma omissão. Pra um Route Handler
futuro (fatia 6, itens 4+) conseguir o access token pra propagar
`Authorization: Bearer` pro backend, criei `lib/auth-token.ts`
(`obterAccessToken`, usa `getToken()` de `next-auth/jwt` — lê o cookie
httpOnly direto, nunca passa pelo endpoint público de sessão).

Renovação de token extraída pra funções puras testáveis
(`lib/auth-token-refresh.ts`: `precisaRenovar`, `renovarToken`) em vez
de ficar tudo dentro do callback `jwt` — mesmo princípio de
testabilidade já seguido no back-end (ex: `LlmProviderFactoryImpl` do
`ai-service`). 7 testes novos com `fetch` mockado.

Logout (`lib/auth-actions.ts`, função `sair()`) faz RP-Initiated Logout
de verdade: `signOut({ redirect: false })` só limpa a sessão local, sem
isso o usuário continuaria "logado" no Keycloak (SSO) e um login
seguinte pularia a tela de senha — então depois redireciona pro
`end_session_endpoint` do Keycloak com `id_token_hint` (identifica o
client sem precisar de `client_id`/`client_secret` — o id_token já
carrega isso no claim `aud`). `id_token` não está em `session`/`auth()`
pelo mesmo motivo do accessToken, então `obterIdTokenParaLogout()`
usa `getToken()` direto ali também, com `secureCookie` derivado de
`NODE_ENV` (replica a regra `useSecureCookies = protocolo https` que o
próprio `@auth/core` usa internamente — dev é sempre http, produção
sempre https via Cloudflare Tunnel, ADR-0019, então `NODE_ENV` é um
proxy confiável pros dois ambientes reais deste projeto especificamente,
não uma heurística genérica de qualquer app).

`proxy.ts` (não `middleware.ts` — Next.js 16 renomeou, achado no item
1) reexporta `auth` com `callbacks.authorized` decidindo: `/login`
sempre liberado, resto exige sessão — retornar `false` já redireciona
sozinho pra `pages.signIn: "/login"` configurado em `auth.ts`. Matcher
exclui `/api/auth/*` (senão quebra o próprio fluxo de login/callback),
`_next/static`, `_next/image`, `favicon.ico`.

Validação completa contra a stack real, não só `npm run build`: dev
server subido com `.env.local` (`AUTH_SECRET` gerado via
`openssl rand -base64 33`, registrado em `security.md`), confirmado via
`curl` que `GET /` sem sessão dá `307` pra `/login`, `GET /login` dá
`200`, `POST /api/auth/signin/keycloak` (com CSRF token de verdade)
redireciona pro Keycloak real com PKCE `code_challenge` e SEM
`client_secret` (prova que o `token_endpoint_auth_method: "none"`
funcionou), e o fluxo completo de login (Keycloak → callback → sessão)
funciona de ponta a ponta.

Criado: `services/web/auth.ts`, `proxy.ts`,
`app/api/auth/[...nextauth]/route.ts`, `app/login/page.tsx`,
`lib/auth-token-refresh.ts`, `lib/auth-token.ts`, `lib/auth-actions.ts`,
`types/next-auth.d.ts`, `.env.example`,
`__tests__/auth-token-refresh.test.ts`. Alterado: `app/page.tsx`
(mostra usuário logado + botão sair, placeholder até o item 3),
`docs/architecture/security.md` (inventário de credenciais —
`AUTH_SECRET`), `docs/tasks.md` (item 2 concluído), `services/web/README.md`.
Removido: `__tests__/page.test.tsx` (a página virou Server Component
`async`, Vitest não suporta — confirmado na própria documentação do
Next.js).

## 2026-08-10 — item 3: layout base (shell)

Pedido: "podemos seguir para o proximo passo" (depois de pedir as
credenciais de dev pra logar — respondidas: `usuario.teste` /
`financas123`, já documentadas no Postman).

Route group `app/(app)/` — recurso do App Router que agrupa rotas sob
um layout compartilhado sem aparecer na URL (`(app)/page.tsx` continua
sendo `/`). Usado pra dar cabeçalho/navegação só pras páginas
autenticadas, deixando `/login` de fora (sem nav, sem botão de sair,
só a tela de entrar). `app/(app)/layout.tsx` novo: nome do sistema,
navegação principal, usuário logado (nome ou e-mail) + botão sair no
cabeçalho — página antiga (`app/page.tsx`, com "logado como"/botão
sair) virou `app/(app)/page.tsx`, mais enxuta, já que o cabeçalho
cobre isso agora, e ganhou o texto de placeholder do Dashboard (item 6).

`lib/nav-items.ts` centraliza os itens de menu com uma flag
`implementado: boolean` — item ainda não construído (Contas,
Transações, Documentos, Chat IA — mapeando pros itens 4/5/7/8 do
backlog) aparece no menu como texto desabilitado com "em breve", não
como link de verdade, pra não ter link morto (404) apontando pra rota
que ainda não existe. Só "Dashboard" é link real.

Layout confirma sessão de novo (`if (!sessao?.user) redirect("/login")`)
mesmo já protegido pelo `proxy.ts` — defensivo pro caso de
`callbacks.jwt` não conseguir renovar o token (`token.error`, ver item
2) e a sessão ainda existir mas sem user válido; se `sessao.error`
estiver presente, mostra um aviso no cabeçalho ("sessão com problema,
entre novamente") em vez de falhar silenciosamente.

`npm run lint`/`test`/`build` limpos depois da mudança. Não precisou
reiniciar o dev server — Turbopack recarregou sozinho, confirmado via
`curl` que `/login` e `/` continuaram respondendo certo.

Criado: `services/web/app/(app)/layout.tsx`,
`services/web/app/(app)/page.tsx`, `services/web/lib/nav-items.ts`.
Removido: `services/web/app/page.tsx` (substituído pelo da rota
agrupada). Alterado: `docs/tasks.md` (item 3 concluído).

## 2026-08-10 — item 4: CRUD de conta (account-service) — primeira tela de dado real

Pedido: "pode seguir pro item 4".

Antes de escrever qualquer componente, li a doc oficial de mutação de
dado do Next.js embutida no pacote
(`node_modules/next/dist/docs/01-app/01-getting-started/07-mutating-data.md`)
e a de fetch (`06-fetching-data.md`) — confirmou que Server Actions
("use server") são o caminho recomendado do App Router pra mutação
disparada por formulário da própria UI, não Route Handler + fetch
client-side. O item 4 do backlog, escrito antes desse item começar,
previa "Route Handler (proxy autenticado)" — desviei conscientemente
pra Server Action, mais simples (sem serialização manual,
`useActionState` já dá pending/erro), e documentei o porquê no próprio
`tasks.md` já que muda o padrão que os itens 5/7/8 vão repetir.

`lib/account-service.ts`: client HTTP `server-only` (mesma proteção já
usada em `lib/auth-token.ts`) pro `account-service`. Reaproveita
`obterAccessToken()` do item 2 — cada chamada propaga
`Authorization: Bearer` do usuário logado, nunca client_credentials
(mesmo princípio de "propagar o token do próprio usuário" já usado em
toda comunicação síncrona entre serviços de back-end,
`overview.md` seção 8). `cache: "no-store"` explícito em todo fetch —
dado financeiro por usuário não pode vazar entre requests de usuários
diferentes via cache do Next.js. `AccountServiceError` carrega o status
HTTP original, pra Server Action decidir se é erro de validação (400)
vs. não encontrado (404) sem precisar re-parsear.

Tela (`app/(app)/contas/page.tsx`, Server Component): busca a lista
direto com `await listarContas()` durante o render — sem Route Handler
intermediário, sem `useEffect`/`useState` de loading no client, o HTML
já chega pronto. `app/(app)/contas/actions.ts`: três Server Actions
(`criarContaAction`/`atualizarContaAction`/`excluirContaAction`), as
duas primeiras no formato `(estadoAnterior, formData) => FormState`
que `useActionState` espera, `revalidatePath("/contas")` depois de
mutar pra lista atualizar sozinha. `conta-form-dialog.tsx` (client):
um dialog só, reutilizado pra criar E editar (troca de campos
via prop `modo`) — detecta "acabou de salvar com sucesso" observando a
transição `pending: true -> false` sem erro (não tem outro jeito de
saber isso só com `useActionState`), fecha o dialog e limpa o form
sozinho. `excluir-conta-button.tsx`: confirmação via `confirm()`
nativo do navegador antes de submeter (só funciona em Client
Component).

`lib/nav-items.ts`: "Contas" virou `implementado: true` — primeiro link
de verdade no menu, os outros continuam "em breve".

**Achado real, mesma classe dos anteriores**: o pacote `server-only`
(usado em `lib/auth-token.ts` desde o item 2 e agora em
`lib/account-service.ts`) depende do bundler do Next.js pra virar
no-op — fora dele, ele **lança erro de propósito** pra impedir uso
client-side. `npm run build` nunca acusou isso (o bundler do Next.js
trata certo), mas o primeiro teste real desses arquivos
(`account-service.test.ts`) quebrou com "This module cannot be
imported from a Client Component module" — o Vitest não tem essa
mágica do Next.js. Resolvido com um alias no `vitest.config.mts`
(`server-only` → um stub vazio, só no ambiente de teste,
`vitest.server-only-stub.ts`) — não muda nada do build/dev real.

Validação contra a stack real: GET `/contas` autenticado (fluxo de
login simulado via `curl`, mesmo roteiro do item 2) trouxe as 10 contas
reais que `usuario.teste` já tinha de testes anteriores desta sessão —
confirma `obterAccessToken()` + propagação de token + parse de
resposta funcionando de ponta a ponta contra o `account-service` de
verdade, não só mock. Mutação (criar/editar/excluir) validada via 5
testes automatizados novos (`fetch` mockado) + `npm run
build`/`lint` limpos — não dá pra simular Server Action via `curl` puro
(protocolo Flight do React exige um header `Next-Action` com encoding
específico não documentado publicamente), então o clique de verdade no
formulário fica pro usuário conferir no navegador.

Criado: `services/web/lib/account-service.ts`,
`services/web/app/(app)/contas/page.tsx`,
`services/web/app/(app)/contas/actions.ts`,
`services/web/app/(app)/contas/conta-form-dialog.tsx`,
`services/web/app/(app)/contas/excluir-conta-button.tsx`,
`services/web/__tests__/account-service.test.ts`,
`services/web/vitest.server-only-stub.ts`. Componentes shadcn/ui
adicionados: `dialog`, `input`, `label`, `select`. Alterado:
`services/web/lib/nav-items.ts` (Contas → implementado),
`services/web/vitest.config.mts` (alias `server-only`),
`services/web/.env.example`/`.env.local` (`ACCOUNT_SERVICE_URL`),
`docs/tasks.md` (item 4 concluído, mais a duplicata de linha corrigida
que tinha ficado de um edit anterior).

## 2026-08-10 — item 5: CRUD de transação (transaction-service)

Pedido: "pode seguir pro item 5".

Mesmo padrão do item 4, aplicado de novo: `lib/transaction-service.ts`
(server-only, `obterAccessToken()`, `cache: "no-store"`), Server
Component pra leitura, Server Actions pra mutação. Página `/transacoes`
ficou com duas seções: transações normais (com filtro) e transações
recorrentes (criar/listar/cancelar — a API não tem `PUT` pra regra
recorrente, então não tem "editar" aqui, diferente de transação avulsa).

Filtro por conta/período implementado como `<form method="GET">`
nativo — sem componente client, sem `useState`/`onChange`. O Server
Component (`page.tsx`) só lê `searchParams` (que nessa versão do
Next.js é uma `Promise`, precisa de `await`) e passa direto pro
`listarTransacoes(filtro)`. Funciona até com JavaScript desabilitado no
navegador — reforça a mesma filosofia de "progressive enhancement" já
usada nos Server Actions dos formulários de criar/editar.

Segunda vez que o padrão "form com `confirm()` nativo antes de
submeter" apareceu (primeira foi excluir conta, item 4; agora cancelar
transação E cancelar regra recorrente) — extraído pra
`components/confirm-action-button.tsx`, reaproveitado nos três lugares
(o `excluir-conta-button.tsx` do item 4 foi refatorado pra usar o
componente novo em vez de duplicar a lógica).

Validado contra a stack real: GET `/transacoes` autenticado (mesmo
roteiro de login via `curl` dos itens 2/4) trouxe as 10 transações e 3
regras recorrentes reais que `usuario.teste` já tinha de testes
anteriores desta sessão; filtro `?contaId=...` testado também (200).
6 testes automatizados novos (`transaction-service.test.ts`) cobrindo
montagem de query string (com/sem filtro), erro 401 sem token, e
propagação de mensagem/status de erro do `transaction-service`
(incluindo 422 de saldo insuficiente). Mutação (registrar/editar/
cancelar transação, criar/cancelar regra) validada só via os testes
mockados + `npm run build`/`lint`, mesmo limite já registrado no item 4
(Server Action não dá pra simular via `curl` puro) — fica pro usuário
conferir clicando na tela.

Criado: `services/web/lib/transaction-service.ts`,
`services/web/app/(app)/transacoes/page.tsx`,
`services/web/app/(app)/transacoes/actions.ts`,
`services/web/app/(app)/transacoes/transacao-form-dialog.tsx`,
`services/web/app/(app)/transacoes/recorrente-form-dialog.tsx`,
`services/web/components/confirm-action-button.tsx`,
`services/web/__tests__/transaction-service.test.ts`. Alterado:
`services/web/app/(app)/contas/excluir-conta-button.tsx` (refatorado
pra usar `ConfirmActionButton`), `services/web/lib/nav-items.ts`
(Transações → implementado), `services/web/.env.example`/`.env.local`
(`TRANSACTION_SERVICE_URL`), `docs/tasks.md` (item 5 concluído).

## 2026-08-10 — item 6: dashboard (PRD 3.7, budget-service)

Pedido: "pode seguir pro item 6".

Último item de dado real da fatia antes do fechamento (upload de
documento e chat de IA, itens 7/8, ainda faltam). Substitui o
placeholder do "Dashboard" criado no item 3. Mesmo padrão dos itens
4/5: `lib/budget-service.ts` novo (server-only, `obterAccessToken()`,
`cache: "no-store"`) + `resumoPorCategoria` adicionado em
`lib/transaction-service.ts` (só faltava esse endpoint, os outros já
existiam desde o item 5).

Seletor de mês usa `<input type="month">` nativo — o valor que o
navegador dá (`AAAA-MM`) já é exatamente o formato que
`budget-service`/`transaction-service` esperam, zero conversão. Como
`resumo-por-categoria` do `transaction-service` trabalha com
`inicio`/`fim` (data, não mês), criei `lib/mes.ts`
(`mesAtual`/`limitesDoMes`) pra derivar o primeiro/último dia do mês
selecionado — testado especificamente pros casos que dão errado se
feito na mão (mês de 30 vs 31 dias, fevereiro bissexto vs não
bissexto), 4 testes.

Gráfico de gastos por categoria: instalei o componente `chart` oficial
do shadcn/ui (Recharts por baixo, `npx shadcn add chart`) — os tokens
`--chart-1` até `--chart-5` já tinham sido definidos lá no item 1
(scaffold da paleta), sem precisar tocar em `globals.css` de novo,
confirma que vieram bem pensados desde o início. Barra dupla por
categoria (`totalGasto` do mês atual vs. `totalGastoPeriodoAnterior`)
— o PRD 3.7 pede "comparação com o período anterior" e o
`transaction-service` já calcula isso desde a fatia 1, só faltava
plotar.

"Disponível pra gastar" não mostra só o número final — lista o
detalhamento item a item que o `budget-service` devolve (saldo de
contas, faturas em aberto, despesas recorrentes, reserva), seguindo a
mesma filosofia de "auditoria/explicação rastreável" do ADR-0026 (o
back-end já foi desenhado pra isso, a tela só precisa expor). Reserva
é um form pequeno inline, não uma lista — é valor único por usuário,
não por mês. Orçamentos seguem o mesmo padrão lista+dialog dos itens
4/5, reutilizando `ConfirmActionButton` (item 5) pro cancelar — editar
só manda `valorLimite`, único campo editável na API (categoria/mês
ficam fixos depois de criado, mesma regra do `AtualizarOrcamentoRequest`).

Validado contra a stack real: GET `/` autenticado (mesmo roteiro de
login via `curl` dos itens 2/4/5) trouxe o "disponível pra gastar" de
verdade do `usuario.teste` (R$ 12.340,00, soma de 9 contas reais) e o
payload do gráfico com `categoria`/`totalGasto` reais no HTML
renderizado — confirma `budget-service` e o `resumoPorCategoria` novo
funcionando de ponta a ponta. 4 testes novos em `budget-service.test.ts`
(401 sem token, querystring do mês, 422 de orçamento duplicado, corpo
do PUT da reserva). 27 testes no total do `web` agora, todos verdes,
`npm run build`/`lint` limpos.

Criado: `services/web/lib/budget-service.ts`, `lib/mes.ts`,
`app/(app)/actions.ts`, `app/(app)/orcamento-form-dialog.tsx`,
`app/(app)/reserva-form.tsx`, `app/(app)/gastos-por-categoria-chart.tsx`,
`__tests__/budget-service.test.ts`, `__tests__/mes.test.ts`.
Componentes shadcn/ui adicionados: `chart`, `card`. Alterado:
`lib/transaction-service.ts` (`resumoPorCategoria` novo),
`app/(app)/page.tsx` (dashboard de verdade, substitui o placeholder do
item 3), `.env.example`/`.env.local` (`BUDGET_SERVICE_URL`),
`docs/tasks.md` (item 6 concluído).

## 2026-08-10 — item 7: upload de documento (document-service)

Pedido: "pode seguir pro item 7".

Primeira vez que o padrão `lib/<serviço>-service.ts` server-only
(items 4/5/6) precisou de uma exceção real: upload é
`multipart/form-data`, não JSON. `chamar()` nos outros três clients
sempre fixa `Content-Type: application/json`; aqui isso quebraria o
upload — `fetch` com um `FormData` no corpo calcula o boundary do
multipart sozinho, e se o `Content-Type` já vier setado na mão (sem
boundary), o `document-service` não consegue parsear as partes.
`lib/document-service.ts` não fixa `Content-Type` nenhum, deixando o
`fetch` decidir — documentado em comentário no código pra não alguém
"corrigir" isso sem entender o motivo depois. Escrevi um teste
específico só pra essa garantia (`document-service.test.ts`, confirma
`headers.has("content-type") === false` antes do envio).

Fluxo em três telas, primeira vez que a fatia 6 usa **polling
client-side de verdade**: `/documentos` (upload + lista) →
upload aceito redireciona pra `/documentos/{id}` (upload é assíncrono
desde a fatia 3 — extração real leva minutos, não segundos) →
`/documentos/[id]` faz `setInterval` a cada 4s chamando uma Server
Action (`buscarDocumentoAction`) direto de um `useEffect`, não presa a
formulário nenhum — mesmo padrão de "refresh data fora de mutação" que
a doc oficial do Next.js mostra (`useEffect` + Server Action pra
view count, lida antes de escrever este item). Para de sondar sozinho
assim que o status sai de `RECEBIDO`/`PROCESSANDO`. Chegando em
`AGUARDANDO_CONFIRMACAO`, mostra os lançamentos com checkbox (todos
marcados por padrão — desmarcar = rejeitar, mesma semântica do
`ConfirmarLancamentosRequest`) + select de conta; `ERRO_PROCESSAMENTO`
mostra a `mensagemErro` direto.

`next.config.ts` ganhou `experimental.serverActions.bodySizeLimit:
"10mb"` — o default do Next.js (1MB) é pequeno demais pra fatura PDF
real (as de teste em `test-data/`, usadas desde a fatia 3, passam
fácil de 1MB); 10mb casa com
`quarkus.http.limits.max-body-size=10M` que o `document-service` já
aceita, então não tem por que o Next.js ser mais restritivo que o
próprio destino.

**Limite de validação registrado com transparência** (primeira vez
que isso precisa ficar mais explícito que nos itens anteriores): o
upload passa por uma Server Action com `<input type="file">`, cujo
protocolo Flight do React é multipart com boundary + action ID de
build — mais específico ainda que uma Server Action JSON comum, não
reproduzível de forma confiável via `curl` no tempo desta sessão
(diferente dos itens 4-6, onde pelo menos o GET foi validado contra a
stack real). GET `/documentos` foi validado contra o `document-service`
real (200, 0 documentos do `usuario.teste` até agora — usuário nunca
tinha importado nada). 5 testes automatizados novos cobrindo o upload
mockado (incluindo a garantia do `Content-Type`) + confirmação/erro
422. Deixei registrado no `tasks.md` que este item tem menos cobertura
automatizada que os anteriores — o upload de PDF de verdade
(`test-data/*.pdf` já existentes) fica pro usuário testar na tela,
merece atenção extra na conferência manual.

Criado: `services/web/lib/document-service.ts`,
`app/(app)/documentos/page.tsx`,
`app/(app)/documentos/actions.ts`,
`app/(app)/documentos/upload-documento-form.tsx`,
`app/(app)/documentos/[id]/page.tsx`,
`app/(app)/documentos/[id]/documento-detalhe.tsx`,
`__tests__/document-service.test.ts`. Alterado: `next.config.ts`
(`bodySizeLimit`), `lib/nav-items.ts` (Documentos → implementado),
`.env.example`/`.env.local` (`DOCUMENT_SERVICE_URL`), `docs/tasks.md`
(item 7 concluído).

## 2026-08-10 — extensão do dashboard: reserva sugerida + cotação do dólar

Pedido: "podemos adicionar funcionalidades como sugerir um valor de
reserva baseado no valor total de receita desse usuario. mostrar o
valor do dollar do dia e quantos R$ reais custa para comprar 1 dollar" —
fora do backlog original da fatia 6, pedido depois do item 7.

Duas decisões reais antes de codar (não são ambiguidade pequena — uma é
regra de negócio nova, outra é integração externa nova), perguntadas
direto ao usuário em vez de assumidas:

1. **Regra da reserva sugerida**: entre "1 mês de receita média",
   "percentual da receita do mês atual" e "múltiplos meses (3-6)", o
   usuário escolheu a primeira (recomendada) — mais simples de explicar
   ("guarda 1 mês de renda"), menos sujeita a oscilação de um único mês
   como a opção de percentual seria.
2. **Fonte da cotação do dólar**: entre AwesomeAPI, Banco Central
   (PTAX oficial) e "outra fonte", o usuário escolheu AwesomeAPI
   (recomendada) — pública, brasileira, sem chave/cadastro, endpoint
   simples (`GET /last/USD-BRL`), mais direta que a API OData do Bacen
   (que além disso só traz cotação do dia útil anterior, não "agora").

**Reserva sugerida**: `lib/reserva-sugerida.ts`
(`calcularReservaSugerida`, função pura — soma só `RECEITA` +
`CONFIRMADA`, divide por `MESES_MEDIA_RECEITA=3`) + `lib/mes.ts` ganhou
`limitesUltimosMeses(mesReferencia, quantidade)` pra calcular a janela
de 3 meses terminando no mês selecionado no dashboard (`new Date` com
`monthIndex` negativo já rola o ano pra trás sozinho — comportamento
padrão do JS, sem precisar tratar virada de ano na mão, testado com
`limitesUltimosMeses("2026-01", 3)` → `2025-11-01`). Dashboard busca
`listarTransacoes` na janela (reaproveita o client do
`transaction-service`, item 5 — nenhum client novo), calcula a
sugestão, passa pro `ReservaForm` como prop. Botão "usar sugestão" só
preenche o input via `ref` — sem round-trip ao servidor, o número já
veio calculado no Server Component.

**Cotação do dólar**: `lib/cambio.ts` — primeira chamada HTTP do
projeto que não é a um dos seis microsserviços. Diferença real de
design em relação aos outros clients: sem `obterAccessToken()` (API
pública, sem autenticação) e sem `cache: "no-store"` — os clients de
microsserviço usam `no-store` de propósito porque são dado por usuário
que nunca pode vazar entre requests; a cotação do dólar é igual pra
todo mundo, então cachear é correto aqui, `next: { revalidate: 300 }`
(5min) evita bater na AwesomeAPI a cada carregamento do dashboard.
Falha da API externa (fora do ar, timeout, resposta malformada) captura
e devolve `null` em vez de propagar erro — cotação é informativa, não
pode derrubar o dashboard inteiro (diferente dos dados financeiros do
próprio usuário, que legitimamente devem falhar alto se o
account/transaction/budget-service estiver fora do ar).

Validado contra a stack real e a API externa de verdade (não só mock):
GET `/` autenticado trouxe a cotação real do dia (~R$5,11/US$1,
confirmada também com `curl` direto na AwesomeAPI) e a reserva sugerida
bateu exatamente com o cálculo manual feito à parte (1 receita
confirmada de R$5.000 na janela de 3 meses → sugestão R$1.666,67,
mesmo valor renderizado na página). 10 testes novos (4
`reserva-sugerida.test.ts`, 3 `limitesUltimosMeses` em `mes.test.ts`, 3
`cambio.test.ts` incluindo os casos de API fora do ar) — 42 testes no
total do `web` agora, `npm run build`/`lint` limpos.

Criado: `services/web/lib/reserva-sugerida.ts`, `lib/cambio.ts`,
`__tests__/reserva-sugerida.test.ts`, `__tests__/cambio.test.ts`.
Alterado: `lib/mes.ts` (`limitesUltimosMeses`), `app/(app)/page.tsx`
(fetch da cotação + da janela de receita, card novo "Dólar hoje"),
`app/(app)/reserva-form.tsx` (prop `sugestao` + botão "usar sugestão"),
`__tests__/mes.test.ts` (3 casos novos), `docs/tasks.md` (nota anexada
ao item 6).

## 2026-08-10 — item 8: refinamento visual (design system)

Pedido: o usuário trouxe um prompt extenso e detalhado (agindo como um
briefing de "Senior Product Designer") pedindo um design system
completo — identidade visual, 40+ componentes documentados, paleta
multi-cor, tipografia, wireframes ASCII, guidelines de UX/UI, tudo
antes de qualquer implementação. Pediu explicitamente: "faça uma
analise e se voce tiver algo que discorde me avise".

**Análise apresentada antes de codar** (não implementei calado, o
pedido explícito era pra eu discordar se achasse necessário):

1. **Escopo do "design system completo" contradiz o princípio central
   deste projeto.** CLAUDE.md é explícito sobre fatias verticais, não
   camadas horizontais completas de uma vez — construir Command
   Palette, Data Grid, Timeline, Step Wizard, Context Menu, Drawer
   (nenhum com uso real hoje) e documentar exaustivamente cada um é
   exatamente o antipadrão que este projeto rejeita desde o início.
2. **O menu lateral sugerido (Metas, Investimentos dedicado,
   Relatórios, Importações, Exportações, Categorias separada,
   Receitas/Despesas separadas de Transações) não corresponde a
   nenhum serviço de back-end nem item do PRD** — teria criado link
   morto ou pressão de escopo pra inventar feature.
3. **Header com busca/notificação é prematuro** — sem
   `notification-service` (ainda 🔲 planejado) nem busca implementada,
   um sino/lupa sem função é decorativo.
4. Dois pontos menores, só confirmando: trocar Geist por Inter (pedido
   explícito), e não expandir a paleta pra "cor de marca" por tipo de
   dado (o próprio prompt pede "evite excesso de cores" — uma paleta
   de 4 cores + escala de cinza já é mais fiel a isso do que roxo pra
   investimento, verde pra meta etc. espalhados pelo produto).

Usuário concordou com a análise inteira e confirmou 3 decisões via
`AskUserQuestion`: documentação viva e incremental (não especificação
antecipada — `docs/architecture/design-system.md` novo, cresce por
tela real construída, não por catálogo teórico), trocar fonte pra
Inter, e encaixar isso como item dedicado (8) antes do chat de IA —
chat vira item 9, fechamento vira item 10 (renumeração no `tasks.md`).

**Implementação, mantendo a mesma disciplina de "só construir o que
tem uso real" que usei na própria análise crítica**:

Tokens (`app/globals.css`): `--radius` 10px→12px ("nunca cantos
retos", mantendo a fórmula de escala já existente do shadcn/ui pros
demais radius). `components/ui/card.tsx` ganhou `shadow-sm` + borda
mais discreta (`ring-foreground/5`, era `/10`) — "sombra extremamente
suave, borda discreta" do prompt, aplicado no único lugar que já
existia (o `Card` do dashboard).

Tipografia (`app/layout.tsx`): Geist → Inter. **Achado real, só visível
lendo o CSS gerado com atenção**: a variável do `next/font` do Geist se
chamava `--font-geist-sans`, mas o bloco `@theme inline` (herdado
do `npx shadcn init` do item 1) sempre referenciou `--font-sans` — nomes
diferentes na cadeia CSS. Resultado: a fonte customizada **nunca esteve
de fato aplicada** em nenhuma tela dos itens 1-7 — `font-sans` caía
silenciosamente no fallback padrão do navegador, sem erro nem aviso
visual óbvio (Geist e a stack padrão de sistema são visualmente
parecidas o bastante pra não chamar atenção). Corrigido nomeando a
variável do `Inter()` exatamente `--font-sans`. `--font-mono` (que
apontava pro Geist Mono, removido) ajustado pra cair no monospace
default do Tailwind, usado só no tooltip do gráfico.

Shell (`app/(app)/app-sidebar.tsx` novo, `app/(app)/layout.tsx`
reescrito): header horizontal do item 3 trocado por menu lateral fixo
(desktop)/off-canvas (mobile) — as referências citadas pelo usuário
(Linear, Vercel, Notion, YNAB) usam sidebar, não header com nav inline,
e isso é estrutural, não só estético. Decisão consciente de **não**
usar o bloco "sidebar" completo do shadcn/ui — ele vem com
collapse-to-icon, atalho de teclado, persistência via cookie, tooltip
por item colapsado: infraestrutura real de dashboard enterprise, sem
uso nenhum pros 5 itens de menu que o produto tem hoje. Mesmo
raciocínio da crítica ao prompt original, aplicado de novo na hora de
implementar. Escrito na mão (~100 linhas): `useState` só pro estado
mobile (aberto/fechado), breakpoint `md:` CSS puro pro comportamento
desktop (sem hook de detecção de mobile — evita flash de layout errado
no primeiro paint), item ativo via `usePathname()` (toque de polish que
o header antigo não tinha).

Padding de página (`p-6` → `p-6 md:p-8`) nas 5 páginas existentes —
mais respiro no desktop ("nada apertado" do prompt), mobile
inalterado. `<main>` do layout ganhou `pt-16 md:pt-0` pra não colidir
com o botão de menu flutuante no mobile.

Validado contra a stack real (mesmo roteiro de login via `curl` dos
itens anteriores): shell novo confirmado renderizando (aria-labels
"Abrir menu"/"Fechar menu" presentes na resposta HTML), fonte Inter
confirmada carregando (referência ao arquivo da fonte no HTML). `npm
run lint`/`test` (42, sem testes novos — mudança é puramente
visual/estrutural, sem lógica nova pra testar)/`build` limpos.

Criado: `docs/architecture/design-system.md`,
`services/web/app/(app)/app-sidebar.tsx`. Alterado:
`services/web/app/globals.css` (radius, comentário sobre o bug de
fonte), `services/web/app/layout.tsx` (Inter), `services/web/components/ui/card.tsx`
(shadow+ring), `services/web/app/(app)/layout.tsx` (shell reescrito),
`services/web/app/(app)/{page,contas/page,transacoes/page,documentos/page,documentos/[id]/page}.tsx`
(padding), `docs/tasks.md` (item 8 novo, chat/fechamento renumerados
pra 9/10).

## 2026-08-10 — fix: token de acesso não renovava nas chamadas aos microsserviços

Pedido: "esta ocorrendo um erro ao tentar anexar uma fatura" — antes de
seguir pro item 9.

Investigado direto no log do dev server (`/tmp/web-dev.log`, mantido
rodando em background durante toda a fatia): não era um bug específico
do upload — o mesmo padrão de `Erro 401` aparecia tanto no
`document-service` (upload) quanto no `transaction-service`
(dashboard), em momentos diferentes da sessão. Isso apontou pra algo
estrutural na autenticação, não pro fluxo de upload em si.

**Causa raiz**: `lib/auth-token.ts` (`obterAccessToken()`, criado no
item 2) usa `getToken()` de `next-auth/jwt` pra ler o cookie httpOnly
direto — decisão certa pra nunca expor o access token via
`/api/auth/session` (ver ADR-0027), mas com uma consequência que só
apareceu na prática: `getToken()` **só decodifica** o cookie, nunca
passa pelo `callbacks.jwt` do `auth.ts`, que é onde a renovação
automática de token foi implementada no item 2. A renovação só
acontecia quando algo chamava `auth()` de verdade (ex: o layout, pra
checar sessão) — qualquer chamada a um microsserviço vinda de
`obterAccessToken()` (todos os quatro clients: account/transaction/
budget/document-service) usava o token cru do cookie, sem checar
validade. Token do Keycloak expira em 5 minutos (`expires_in: 300`,
mesmo valor documentado desde a fatia 1 no Postman) — qualquer sessão
viva além disso passava a tomar 401 em toda chamada, incluindo o
upload de fatura.

**Fix**: `obterAccessToken()` agora checa `precisaRenovar()` e chama
`renovarToken()` ele mesmo (reaproveitando as mesmas funções puras já
testadas do item 2, `lib/auth-token-refresh.ts`) antes de devolver o
token. Envolvido em `cache()` do React (memoização por request via
AsyncLocalStorage, segura pra dado multi-tenant) — sem isso, o
dashboard chamaria `renovarToken()` até 6 vezes em paralelo
(`Promise.all` de account/transaction/budget-service rodando junto),
todas com o mesmo `refresh_token` ainda não trocado.

**Limitação conhecida, aceita conscientemente**: o token renovado por
essa função não é persistido de volta no cookie — não dá mesmo pra
fazer isso a partir de um Server Component (Next.js só permite
`cookies().set()` em Server Action/Route Handler/proxy, não em
render), então cada request depois dos 5 minutos iniciais volta a
chamar `renovarToken()` de novo. Testado se isso quebra na prática:
peguei um `refresh_token` real via `curl` (mesmo roteiro de login já
usado nesta sessão) e chamei `grant_type=refresh_token` duas vezes
**com o mesmo token antigo** contra o Keycloak real — as duas vezes
devolveram 200. Confirma que o realm `financas` não tem rotação/
revogação de refresh token ativada (`revokeRefreshToken` no default do
Keycloak, nunca setado explicitamente em
`infra/keycloak/realm-financas.json`) — reusar o token repetidamente é
seguro aqui, só um pouco redundante (uma chamada a mais ao Keycloak por
request depois de expirado). Se o realm um dia ativar rotação, essa
função precisa evoluir pra persistir o token renovado (via
Server Action ou reforçando o refresh dentro do próprio `proxy.ts`).

`npm run lint`/`test` (42, sem teste novo — a lógica de refresh em si
já era testada, a mudança foi só onde ela é chamada)/`build` limpos.

Alterado: `services/web/lib/auth-token.ts` (`obterAccessToken()`
renova sozinho, com `cache()`).

## 2026-08-10 — item 9: chat com a IA (ai-service)

Pedido: "pode seguir pro item 9" (depois do fix do token e da opinião
sobre o serviço de log).

Último item de dado real da fatia 6 — só falta o fechamento (item 10).
`lib/ai-service.ts` no mesmo padrão server-only dos outros quatro
clients. Estrutura: `app/(app)/chat/layout.tsx` (sidebar com lista de
conversas + botão de configuração), `app/(app)/chat/page.tsx` (conversa
nova) e `app/(app)/chat/[id]/page.tsx` (conversa existente) — os dois
renderizam o mesmo `ChatClient`.

Primeira tela da fatia que não encaixa no padrão "Server Action presa a
form + `useActionState`" dos itens 4-7: o chat precisa de uma lista de
mensagens que cresce a cada troca, não um estado único de
pending/erro. `ChatClient` (client component) mantém a lista em
`useState` local, chama `enviarMensagemAction` direto (não presa a
`<form>`), mostra a mensagem do usuário otimisticamente antes da
resposta do agente chegar. Primeira mensagem de uma conversa nova
atualiza a URL pra `/chat/{conversaId}` via `router.replace()` — sem
reload, sem perder o estado já renderizado.

Proposta de ação (`tipo=PROPOSTA_ACAO` na resposta) vira um card dentro
da bolha de mensagem com um botão "Confirmar" — que só envia a mensagem
literal "sim", não existe endpoint de confirmação separado (o
`ai-service` foi desenhado assim de propósito desde a fatia 5:
confirmação é conversacional). Corrigir é só digitar outra coisa, sem
botão dedicado — reflete a mesma simplificação já registrada na fatia 5
(correção vira comando novo do zero, não merge incremental com a
proposta anterior).

`buscarConfiguracaoIa` (chamada tanto no layout quanto na página em
toda renderização) envolvida em `cache()` do React pra dedupe — mesmo
princípio já aplicado em `lib/auth-token.ts` mais cedo nesta sessão,
reaproveitado aqui na primeira vez que apareceu de novo a mesma
situação (duas chamadas HTTP idênticas na mesma request).

**Validação de ponta a ponta com o LLM de verdade, não só a API REST**
— primeira vez nesta fatia que isso foi possível: configurei Ollama
como provedor via API direta (usuário ainda não tinha configurado
nada), mandei uma pergunta real pro `ai-service`
("quanto tenho disponivel pra gastar esse mes?"). Primeira tentativa
deu timeout de 30s — `llama3.1` ainda frio no Ollama, achado que já
era esperado (mesmo comportamento visto na fatia 5, modelo precisa
carregar em memória na primeira chamada). Segunda tentativa, modelo já
quente, respondeu em 8s: "Você tem R$32508.67 disponível pra gastar em
2026-08", com `trace` confirmando a tool `buscar_saldo_disponivel`
invocada — o mesmo valor exato já confirmado no dashboard (item 6),
prova que RAG/tool-calling do agente está consultando o dado real do
usuário, não alucinando. Carreguei essa conversa de verdade em
`/chat/{id}` via `curl` autenticado (mesmo roteiro de login de sempre)
e confirmei no HTML renderizado que a resposta aparece — primeira vez
na fatia 6 que um fluxo de IA foi validado ponta a ponta pela UI, não
só pela API isolada.

5 testes automatizados novos (`ai-service.test.ts`): 401 sem token,
`conversaId: null` numa conversa nova vs. propagado numa existente, 422
sem provedor configurado, corpo do PUT de configuração. `npm run
lint`/`test` (47)/`build` limpos.

Criado: `services/web/lib/ai-service.ts`,
`app/(app)/chat/{layout,page,actions,chat-client,configuracao-ia-dialog}.tsx`,
`app/(app)/chat/[id]/page.tsx`, `__tests__/ai-service.test.ts`.
Alterado: `services/web/lib/nav-items.ts` (Chat IA → implementado),
`.env.example`/`.env.local` (`AI_SERVICE_URL`), `docs/tasks.md` (item 9
concluído).

## 2026-08-10 — fix: upload de fatura ainda dava erro (dois bugs reais no document-service)

Pedido: "ainda ocorre o erro ao tentar adicionar uma fatura" — depois do
fix de token do turno anterior, que resolveu os 401 mas não era a causa
do erro no upload.

Log do dev server (`/tmp/web-dev.log`) mostrou que o 401 realmente tinha
sumido — o erro agora era **500 no `document-service`**, do lado do
back-end, não do front-end que acabei de construir na fatia 6. Dois
bugs reais, achados na sequência, os dois em código de fatias
anteriores (fatia 3) nunca exercitado dessa forma antes:

**Bug 1 — transação JTA "vazando" pro MongoDB.**
`docker logs financas-document-service` mostrou
`com.mongodb.MongoQueryException: ... 'Transaction numbers are only
allowed on a replica set member or mongos'`. Causa:
`DocumentoRepositoryImpl.salvar()`/`buscarPorId()` tinham
`@Transactional` (JTA) no método inteiro, que grava tanto no MongoDB
(documento) quanto no MySQL (lançamentos, via Hibernate/Panache) — e o
`document-service` é o único dos serviços com Mongo que também tem
Hibernate ORM/MySQL (por isso `ai-service`, 100% Mongo sem JTA nenhum,
nunca teve esse problema, confirmado comparando os dois: minhas
conversas de chat gravaram no Mongo sem erro nenhum na sessão
anterior). Com JTA ativo (por causa do lado MySQL), o Quarkus tenta
enlistar a sessão do Mongo na mesma transação — e sessão/transação no
Mongo exige replica set, que o `mongo` deste projeto não é (roda
standalone, decisão consciente, nunca precisou disso antes).

Corrigido isolando a parte MySQL num `QuarkusTransaction.requiringNew()`
programático (não anotação — `@Transactional` em método privado
chamado de dentro da própria classe nem funcionaria, CDI/interceptor
não pega auto-invocação), deixando as chamadas ao Mongo completamente
fora de qualquer transação JTA. 59 testes do `document-service`
continuam verdes depois da mudança.

**Bug 2 — timeout do REST client mascarando o `@Timeout` configurado.**
Corrigido o bug 1, testei o upload de verdade (`curl` multipart contra
o `document-service` real, fatura de teste de `test-data/`) — aceitou
(202), mas o processamento em background terminava em
`ERRO_PROCESSAMENTO` com "timeout period of 30000ms" nos logs. Achado:
`OllamaRestClient.gerar()` já tinha `@Timeout(120s)` do SmallRye Fault
Tolerance (fatia 3, comentário explícito "timeout generoso"), mas o
REST client do Quarkus tem seu próprio `read-timeout` no nível de
conector, default 30s — e o menor dos dois sempre vence. Resultado: o
`@Timeout` de 120s nunca teve efeito nenhum desde que foi escrito, todo
request pro Ollama morria em 30s calados. Corrigido configurando
`quarkus.rest-client.ollama.read-timeout` explicitamente, acima do
`@Timeout`.

Mesmo depois de corrigido, 120s não foi suficiente pra extrair a fatura
de teste real (processamento passou dos 120s de verdade e ainda
assim não terminou) — subi pra 300s, testei nesse patamar, ainda
insuficiente. Subi pra 600s (10min) como margem generosa. Validação
final: upload real da fatura de teste (`fatura_teste_nubank.pdf`) via
`curl` multipart contra o `document-service` reconstruído, documento
criado às 00:08:13 e processado com sucesso às 00:15:10 (~7min) —
status final `AGUARDANDO_CONFIRMACAO`, 13 lançamentos extraídos
corretamente (descrição, valor, data, categoria sugerida), dentro da
janela de 600s. Extração de fatura completa em CPU é bem mais lenta
que uma pergunta curta de chat (~8s, ver item 9) — registrado como nota
no Javadoc de `OllamaRestClient` que, se uma fatura maior algum dia
estourar mesmo os 600s, o gargalo real passa a ser velocidade de
inferência em CPU, não configuração de timeout, e aí vale considerar
modelo menor/quantizado antes de só subir o número de novo.

Alterado:
`services/document-service/src/main/java/.../infrastructure/persistence/DocumentoRepositoryImpl.java`
(`QuarkusTransaction` programático em vez de `@Transactional` no método
inteiro),
`services/document-service/src/main/java/.../infrastructure/llm/OllamaRestClient.java`
(`@Timeout` 120s→600s),
`services/document-service/src/main/resources/application.properties`
(`quarkus.rest-client.ollama.read-timeout` novo, 610000ms).

## 2026-08-11 — regra de negócio: fatura de cartão vira UMA despesa só, nunca uma transação por lançamento

Pedido explícito do usuário: "a fatura do cartão de crédito entra sempre
como despesa, e a despesa é o valor total da fatura. quando a fatura tem
o nome de duas pessoas [...] o valor total é só da pessoa no qual o nome
é informado [...] o processo de fazer upload da fatura deve ser
assíncrono e deve aparecer para o usuário uma barra de progressão". Duas
decisões alinhadas antes de implementar (AskUserQuestion): filtro do
titular **automático** pelo nome do perfil Keycloak (não confirmação
manual), e barra de progresso **indeterminada** (sem streaming do Ollama
— não vale o esforço pra uma % que não seria real de qualquer forma).

**Antes**: cada lançamento extraído da fatura virava sua própria
`Transacao` ao ser confirmado (evento Kafka carregava a lista inteira de
itens selecionados, `transaction-service` criava uma transação por
item). **Depois**: `DocumentoImportado.getDespesaConsolidada()` (novo)
soma os lançamentos confirmados pelo usuário (despesas menos estornos) e
retorna UM valor líquido só; `DocumentoEventPublisherImpl` publica um
evento com lista de 1 item (mantém o schema Kafka igual de propósito —
`transaction-service` nem precisou mudar, o loop dele já cria 1
`Transacao` pra lista de 1). A seleção item-a-item na tela de
confirmação continua existindo (o usuário ainda pode desmarcar um
lançamento que não é dele antes de confirmar) — só o resultado final
mudou de N transações pra 1.

**Filtro automático do titular**: a extração já sabia restringir a uma
seção de nome específico (`nomeFiltro` em `AgenteExtracaoFaturaService`,
de uma sessão anterior) mas era um campo manual no formulário de upload.
Trocado por `DocumentoResource.nomeUsuarioAutenticado()`, que lê o claim
`name` do token OIDC — nunca mais pedido ao usuário. Campo `nomeFiltro`
removido do form (`UploadDocumentoForm`, spec OpenAPI,
`upload-documento-form.tsx`, `lib/document-service.ts`).

**Achado ao validar**: o usuário de teste do Keycloak (`usuario.teste`)
tinha nome "Usuario Teste" — não batia com nenhum nome da fatura real
usada nos testes deste projeto (titular "João Paulo Santos" +
uma segunda pessoa). Atualizado `firstName`/`lastName` do usuário pra
"João"/"Paulo Santos" (no realm import `realm-financas.json` e via
Admin API no Keycloak já rodando) — é o dono real dos dados de teste
usados no projeto, então alinhar o nome do usuário de teste com o nome
real da fatura é o que faz esse fluxo ser testável de ponta a ponta de
verdade.

**Validação real** (upload real da fatura de teste via curl, contas e
Keycloak reais): extração foi de 13 lançamentos (sem filtro) pra 11 (com
filtro automático pelo nome "João Paulo Santos") — os 2 removidos
são da segunda pessoa da fatura. Confirmação gerou exatamente 1
`Transacao` ("Fatura de cartão de crédito — fatura_teste_nubank", DESPESA,
R$ 1.000,00) e o saldo da conta caiu de R$ 5.000,00 pra R$ 4.000,00 —
diferença bate exatamente com a soma líquida dos 11 lançamentos.

**Dois bugs a mais achados durante essa validação** (nenhum introduzido
nesta sessão, os dois já existiam):
1. `ConfirmarLancamentosUseCase.executar()` tinha `@Transactional`
   envolvendo Mongo + a chamada HTTP ao account-service — mesma causa
   raiz do bug de upload corrigido antes (JTA + Mongo standalone) mas
   nunca tinha sido exercitado contra o Mongo real do docker-compose
   (só contra o Mongo de Dev Services dos testes, que sobe com replica
   set — por isso os testes nunca pegaram isso). Corrigido removendo a
   anotação, mesmo padrão do fix anterior em `DocumentoRepositoryImpl`.
2. O container do `transaction-service` rodando no docker-compose local
   estava desatualizado — nem chegou a inicializar o consumer Kafka do
   tópico `documento.lancamentos-confirmados` (log de startup só
   mostrava o producer de `transacao.eventos`, sem nenhuma linha do
   consumer). `kafka-consumer-groups --list` confirmava: nenhum grupo
   `transaction-service` existia. Resolvido reconstruindo a imagem
   (`docker compose build transaction-service` a partir de um
   `./mvnw package` fresco) — não foi um bug de código, só um container
   local desatualizado em relação ao código já commitado.

**Progresso indeterminado**: novo `components/ui/progress.tsx` (barra
com faixa animada, `role="progressbar"`, sem % — não existe % real pra
mostrar) usado em `/documentos/[id]` enquanto o status é
RECEBIDO/PROCESSANDO. Tela de confirmação ganhou total ao vivo (soma
recalculada conforme o usuário marca/desmarca itens) e a tela CONFIRMADO
ganhou um resumo "Despesa gerada na conta: R$X" no topo, antes da lista
de composição.

Criado: `services/document-service/.../domain/DespesaConsolidada.java`,
`.../domain/ValorFaturaInvalidoException.java`,
`.../infrastructure/rest/ValorFaturaInvalidoExceptionMapper.java`,
`services/web/components/ui/progress.tsx`.
Alterado: `DocumentoImportado.java` (`getDespesaConsolidada`),
`DocumentoEventPublisherImpl.java`, `ConfirmarLancamentosUseCase.java`
(sem `@Transactional`), `DocumentoResource.java`
(`nomeUsuarioAutenticado`), `UploadDocumentoForm.java` (sem
`nomeFiltro`), `AgenteExtracaoFaturaService.java` (Javadoc),
`docs/specs/document-service.yaml`,
`transaction-service/.../ProcessarLancamentosConfirmadosUseCase.java`
(Javadoc), `infra/keycloak/realm-financas.json` (nome do
`usuario.teste`), `services/web/app/(app)/documentos/{actions.ts,
upload-documento-form.tsx, [id]/documento-detalhe.tsx}`,
`services/web/lib/document-service.ts`, `services/web/app/globals.css`
(keyframe), `docs/architecture/design-system.md`. Testes novos:
`DocumentoImportadoTest` (2), `DocumentoResourceTest` (1) no
document-service; `document-service.test.ts` atualizado no web. 62
testes Java (document-service) + 47 testes web, todos verdes.

## 2026-08-11 — pedido: parcelamento sem duplicar entre uploads + IA respondendo sobre cartão/parcelas

Pedido do usuário, verbatim (resumido — regra de negócio + lista de
perguntas que a IA precisa saber responder):

> "nova regra para faturas [...]: se ao fazer upload de uma fatura de
> cartão o usuário já tiver feito uma antes, levar em consideração
> somente as novas compras tanto à vista como parcelada. As compras
> parceladas, independente de novos uploads, as parcelas devem seguir
> sendo reduzidas na data de vencimento da fatura — por exemplo, se fiz
> upload de uma fatura que tinha somente 1 compra parcelada no valor de
> 10 parcelas de R$100,00, no próximo vencimento dessa fatura, em vez de
> ter 10 parcelas, terá somente 9, e assim vai seguindo até acabar todas
> as parcelas."
>
> Perguntas que a IA (Ollama) precisa saber responder:
> 1. Quantas compras parceladas eu tenho atualmente?
> 2. Qual é o valor da fatura desse mês somente de compras parceladas?
> 3. Qual é o valor da fatura total do mês XXXX?
> 4. Qual é o valor da fatura desse mês só de compras não parceladas?
> 5. Qual é o maior valor de parcela que eu tenho no cartão?
> 6. Quantas parcelas faltam para finalizar a compra XXXXX?
> 7. Qual é o valor estimado de compras parceladas para o próximo mês?
> 8. Qual foi o tipo de gasto que mais tive esse mês (comida, diversão,
>    roupas etc.)?
> 9. "No que você consegue me ajudar?" (pergunta de capacidade geral)
> 10. + outras perguntas gerais sobre cartão de crédito e sobre receitas
>     e despesas.

**Achado antes de implementar** (investigação do código existente, não
assumido): o projeto **já tem** um `card-service` inteiro (fatia 2,
entregue em 2026-08-09) que modela exatamente esse comportamento —
`Cartao` (com `diaFechamento`/`diaVencimento`), `Fatura` (uma por
cartão+competência, `StatusFatura` ABERTA/FECHADA/PAGA) e `Parcela`
(uma parcela de uma compra, agrupada por `compraId`,
`numeroParcela`/`quantidadeParcelas`). `LancarCompraUseCase` já
distribui uma compra parcelada em `Parcela`s consecutivas em `Fatura`s
futuras (criadas sob demanda), e `FecharFaturasVencidasJob` (agendado)
já fecha a fatura vencida automaticamente — ou seja, "a parcela 8/11
vira 9/11 no mês seguinte, sem precisar de novo upload" é
**exatamente** o que esse motor já faz, pra compra lançada manualmente.

O problema é que o fluxo de **upload de PDF** (`document-service`,
fatia 3) nunca foi conectado a esse motor — ele extrai lançamentos via
LLM e, desde a mudança de ontem (2026-08-10), publica direto UMA
despesa agregada pro `transaction-service`, sem nunca passar por
`Cartao`/`Fatura`/`Parcela`. Isso significa que os dois requisitos do
pedido (não duplicar compra já conhecida + parcela decrescendo sozinha)
já existem prontos no `card-service` — o trabalho real é **religar o
upload de PDF a esse motor**, não construir um novo, o que evita
duplicar uma máquina de parcelamento que já foi testada (82 testes) e
validada em produção local.

Gaps identificados que a integração precisa resolver:
- Upload de fatura hoje não pergunta a qual `Cartao` cadastrado ela
  pertence (não existe `cartaoId` no formulário/command) — precisa de
  um.
- Sem `compraId` impresso no PDF, dedup entre uploads consecutivos
  precisa de uma heurística de casamento (proposta: descrição-base +
  valor da parcela + quantidade de parcelas, já que a extração já
  reconhece "Parcela X/Y" na descrição).
- `ai-service` (fatia 5) hoje só tem uma tool `FATURA_CARTAO` que
  devolve o total da fatura fechada mais recente — nenhuma pergunta
  granular sobre `Parcela` (das 10 perguntas acima, nenhuma é
  respondível hoje). O padrão MCP documentado em `ai-strategy.md` §4
  ainda não é function-calling de verdade — é um prompt de
  classificação que escolhe um nome de tool de uma lista fechada
  (`ToolConsulta`), despachado em Java. Novas perguntas = novos valores
  de enum + novos métodos, mesmo padrão já usado.

Dado que isso reabre uma decisão de arquitetura que atravessa 3 serviços
(document-service, card-service, ai-service) e potencialmente reverte
parte do que foi testado ontem (a "despesa única" direto pro
transaction-service), a implementação foi pausada pra alinhar o desenho
com o usuário antes de codar.

**Decisão confirmada pelo usuário**: religar pro card-service (opção
recomendada). ADR-0028 escrita (`docs/architecture/adr/0028-upload-fatura-integra-card-service.md`),
supersede o item 1 da ADR-0023. Implementação em 5 partes:

1. **`card-service`**: `ListarComprasUseCase` novo + `GET
   /api/v1/cartoes/{id}/compras` — agrupa `Parcela`s por `compraId`,
   devolve `parcelasRestantes`/`valorTotalRestante` (só parcelas em
   fatura ainda ABERTA). Base pro dedup do document-service e pras tools
   de IA. 88 testes (+6).
2. **`document-service`**: `cartaoId` obrigatório no upload;
   `LancamentoPendente` ganhou `numeroParcela`/`quantidadeParcelas`
   (LLM extrai direto do texto "Parcela X/Y", com fallback via regex se
   o LLM omitir); `CardServiceClient` novo port (`lancarCompra`,
   `listarComprasAtivas`); `ConfirmarLancamentosUseCase` reescrito —
   lança compra nova (à vista ou parcelada) por lançamento confirmado,
   pula os já conhecidos (dedup) e os do tipo RECEITA (card-service
   ainda não modela estorno). Entrar no meio de uma sequência de
   parcelas (ex: primeiro upload já mostra "Parcela 8/11") registra só
   as parcelas restantes. `AccountServiceClient`/`ContaNaoEncontradaException`
   removidos (só existiam pro fluxo antigo). `DespesaConsolidada`/
   `ValorFaturaInvalidoException` (de ontem) removidos — mortos depois
   da mudança. 68 testes.
3. **`web`**: `lib/card-service.ts` novo, página `/cartoes` mínima
   (listar + criar cartão, mesmo padrão de `/contas` — necessária pro
   usuário ter um cartão pra escolher no upload), Select de cartão
   obrigatório no formulário de upload, tela de confirmação/detalhe
   reescrita (mostra "Parcela X/Y" por item, resumo "total a lançar no
   cartão" em vez de "despesa gerada" — resultado agora é compra no
   cartão, não transação direta). 52 testes.
4. **`ai-service`**: 4 tools novas — `compras_parceladas` (quantas
   ativas, maior parcela, quanto falta de cada uma — não filtra por
   nome de compra específica, devolve a lista inteira, simplificação
   consciente pra não precisar de mais uma extração de parâmetro livre
   do LLM), `valor_fatura_mes` (total de um mês, separado em parcelado
   vs à vista), `categoria_que_mais_gastou` (combina transaction-service
   **e** card-service — compra de cartão não gera `Transacao`, só é
   debitada ao pagar a fatura, então ignorar o card-service
   subestimaria qualquer usuário que usa cartão) e
   `capacidades_do_assistente` (resposta fixa). 68 testes (+4).
5. **Docs**: ADR-0028, `docs/architecture/overview.md` (seção 3
   reescrita), `diagrams.md` (container + ER do document-service),
   `ai-strategy.md` (tabela de tools), `docs/specs/card-service.yaml` e
   `document-service.yaml` atualizados, `docs/tasks.md`/`roadmap.md`
   com nota apontando pra essa entrada.

**Três bugs reais achados durante a validação de ponta a ponta** (upload
real da fatura de teste, confirmação real, perguntas reais no chat via
Ollama — não só teste com mock):

1. **Dedup quebrava exatamente no caso mais comum.** Design original
   comparava descrição-base + valor da parcela + quantidade de parcelas.
   Testando na prática: uma compra registrada no meio da sequência (ex:
   "Parcela 8/11") fica guardada no card-service com só as parcelas
   RESTANTES (4, não 11) — mas o PDF de um próximo mês sempre mostra o
   total ORIGINAL fixo ("9/11", nunca muda). Comparar quantidadeParcelas
   quebrava o reconhecimento de "já vi essa compra antes" bem no
   cenário que o usuário pediu pra resolver. Corrigido comparando só
   descrição-base + valor da parcela — `CompraExistente` perdeu o campo
   `quantidadeParcelas` (não tinha mais uso). Validado na prática:
   upload da fatura real → 11 compras lançadas certo (parcelas restantes
   calculadas corretamente pra cada uma) → re-upload da MESMA fatura →
   confirmação → **zero duplicatas** (13 compras antes e depois).
2. **`ai-service` tinha o mesmo bug de timeout do Ollama corrigido ontem
   no document-service** (`@Timeout` do SmallRye Fault Tolerance sem
   efeito porque falta `quarkus.rest-client.ollama.read-timeout` — o
   default do conector, 30s, sempre vencia). Nunca tinha aparecido
   porque a classificação de intenção é uma chamada curta, quase sempre
   sob 30s — só apareceu agora porque o Ollama estava sob carga real
   (vários rebuilds de container em paralelo, ~578% CPU, uma chamada
   trivial de "aquecimento" levou 89s). Corrigido com
   `quarkus.rest-client.ollama.read-timeout=130000`.
3. **Prompt de classificação confundia a tool nova
   `capacidades_do_assistente`** — o LLM às vezes colocava o nome da
   tool no campo `intent` em vez de `tool` (ex: `{"intent":
   "capacidades_do_assistente", "tool": null}`), porque a frase "no que
   você pode me ajudar" soa como uma categoria própria, não uma
   sub-tool de "CONSULTA". Corrigido acrescentando um exemplo explícito
   no prompt mostrando o formato certo — testado direto contra o Ollama
   antes e depois da mudança pra confirmar.

**Validação final, tudo contra containers reais**: upload de fatura real
(`fatura_teste_nubank.pdf`) com `cartaoId` obrigatório → 11 lançamentos
extraídos com `numeroParcela`/`quantidadeParcelas` corretos → confirmação
lançou 11 compras novas no card-service (parcelas restantes calculadas
certo pra quem entrou no meio da sequência) → re-upload da mesma fatura
→ confirmação → zero duplicatas (dedup funcionando) → 4 perguntas reais
no chat via Ollama, todas corretas: "quantas compras parceladas eu
tenho?" (listou 2 com valores e parcelas restantes certos), "qual o
valor da fatura desse mês?" (separou parcelado de à vista certo), "qual
categoria eu mais gastei?" (combinou as duas fontes certo) e "no que
você pode me ajudar?" (resposta fixa certa, depois do fix do prompt).

Criado: `services/card-service/.../application/{CompraResumo,ListarComprasUseCase}.java`,
`.../infrastructure/rest/dto/CompraResumoResponse.java`,
`services/document-service/.../domain/{CardServiceClient,CartaoNaoEncontradoException,CompraExistente}.java`,
`.../infrastructure/client/{CardServiceClientImpl,CardServiceUsuarioClient}.java`,
`.../infrastructure/client/dto/{CompraResumoDto,LancarCompraRequestDto}.java`,
`.../infrastructure/rest/CartaoNaoEncontradoExceptionMapper.java`,
`.../db/migration/V2__add_parcelamento_lancamentos_pendentes.sql`,
`services/ai-service/.../domain/{CompraResumo,Parcela}.java`,
`.../infrastructure/client/dto/{CompraResumoDto,FaturaDetalheDto,ParcelaDetalheDto}.java`,
`services/web/lib/card-service.ts`, `services/web/app/(app)/cartoes/{page.tsx,actions.ts,cartao-form-dialog.tsx}`,
`docs/architecture/adr/0028-upload-fatura-integra-card-service.md`.
Alterado: `card-service/CartaoResource.java` (endpoint novo),
`docs/specs/card-service.yaml`; `document-service/{LancamentoPendente,DocumentoImportado}.java`,
`ConfirmarLancamentosUseCase.java` (reescrito), `DocumentoResource.java`
(cartaoId), `UploadDocumentoForm.java`, `AgenteExtracaoFaturaService.java`
(prompt), `LancamentoExtraidoDto.java` (numeroParcela/quantidadeParcelas
+ regex fallback), `DocumentoEventPublisherImpl.java` (revertido pra
forma genérica, sem chamador hoje), mappers/entities de persistência,
`docs/specs/document-service.yaml`; `ai-service/CardServiceClient.java`
(+2 métodos), `CardServiceRestClient.java`, `CardServiceClientImpl.java`,
`ToolConsulta.java` (+4), `AgenteOrquestradorUseCase.java` (prompt +4
tools), `application.properties` (read-timeout do Ollama);
`services/web/lib/{document-service.ts,nav-items.ts}`,
`app/(app)/documentos/{actions.ts,upload-documento-form.tsx,page.tsx,[id]/{page.tsx,documento-detalhe.tsx}}`;
`docs/architecture/{overview.md,diagrams.md,ai-strategy.md,adr/0023-...,adr/0025-...}`,
`docs/{tasks.md,roadmap.md}`, `docker-compose.yml` (document-service
depende de card-service, não mais account-service), `.env.example`/
`.env.local` (`CARD_SERVICE_URL`). Removido:
`document-service/domain/{AccountServiceClient,ContaNaoEncontradaException,
DespesaConsolidada,ValorFaturaInvalidoException}.java`,
`.../infrastructure/client/{AccountServiceClientImpl,AccountServiceUsuarioClient}.java`,
`.../infrastructure/client/dto/ContaDto.java`,
`.../infrastructure/rest/{ContaNaoEncontradaExceptionMapper,ValorFaturaInvalidoExceptionMapper}.java`.
Testes dos serviços tocados, todos verdes: 88 no card-service (+6), 68 no
document-service, 68 no ai-service (+4), 52 no web. `transaction-service`
só ganhou um ajuste de Javadoc (comentário desatualizado), sem mudança de
comportamento — suite dele não re-executada.

## 2026-08-11 — Fechamento da fatia 6: CI, Docker Compose, docs

Pedido: "continue de onde parou" — item 10 (último) do backlog da fatia 6
em `docs/tasks.md`, sem escopo novo do usuário: CI (`web`), serviço `web`
no `docker-compose.yml`, `.env.example`, `diagrams.md`, `overview.md`,
`roadmap.md`, `README.md` raiz.

Feito: job `web` novo em `ci.yml` (único job Node.js do CI — `npm ci`,
lint, testes, `npm audit --omit=dev` como gate de vulnerabilidade —
`security.md` seção 6/CLAUDE.md princípio 7, sem OWASP Dependency-Check
aqui, é ferramenta Java —, build de produção, build de imagem Docker de
validação), `web` no path filter do job `changes`. Serviço `web` novo no
`docker-compose.yml` (porta 3000, depende dos seis serviços + Keycloak).
`diagrams.md`: `web` no container graph com `BudgetSvc` (gap real
encontrado — o dashboard já usa `lib/budget-service.ts` desde o item 6,
mas a aresta nunca tinha sido desenhada). `overview.md`: tabela de
clientes ganhou porta/status, nota de UX desatualizada removida (já
existe `design-system.md` desde o item 8). `roadmap.md`/`README.md` raiz
atualizados (fatia 6 → Entregue, estrutura de pastas, seção "Estado
atual", URL do `web`, contagem de serviços no CI/compose seis→sete).

**Dois bugs reais achados rodando o `web` como container pela primeira
vez** (nunca tinha sido testado fora de `npm run dev`, só contra
`localhost` sem Docker de permeio) — só apareceram ao validar de
verdade, não em teste automatizado:

1. **Keycloak inalcançável de dentro do container.** O servidor Next.js
   (troca de código por token, renovação, userinfo, jwks) roda dentro do
   container `web`, que não alcança `localhost:8080` (é o próprio
   container — mesma classe de problema já resolvida nos serviços Java
   desde a fatia 1 com `discovery-enabled=false` + path explícito). O
   Auth.js exige solução um pouco diferente: só trocar a URL do
   `issuer` não bastaria, porque com discovery OIDC ligado o discovery
   document do Keycloak sempre devolve URL "localhost" (mesmo
   `KC_HOSTNAME=localhost` que estabiliza o claim `iss`), inalcançável
   de dentro do container. Corrigido lendo o código-fonte real do
   `@auth/core` (`lib/actions/callback/oauth/callback.ts` e
   `.../signin/authorization-url.ts`): informar `token`/`userinfo`/
   `jwks_endpoint` explícitos no provider Keycloak faz o Auth.js pular a
   chamada de discovery inteiramente. `AUTH_KEYCLOAK_ISSUER` (público)
   segue usado só pro `issuer`/redirect de login-logout, que o navegador
   precisa alcançar direto; `AUTH_KEYCLOAK_INTERNAL_ISSUER` (nova,
   opcional — cai no valor público se ausente, caso de dev local sem
   Docker) alimenta os três endpoints de servidor + a chamada de refresh
   de token em `lib/auth-token.ts`. Validado via roteiro de `curl`
   simulando o navegador inteiro (GET `/api/auth/csrf` → POST
   `/api/auth/signin/keycloak` com CSRF real → segue o redirect pro
   Keycloak → POST no formulário de login (`usuario.teste`/
   `financas123`) → segue o redirect de volta pro callback do `web`) —
   contra o container real, token trocado com sucesso.
2. **Cookie de sessão nunca encontrado, mesmo com o token trocado
   certo.** `GET /` autenticado dava 500 ("Sessão sem access token
   válido"). Achado por instrumentação temporária (`console.error` no
   `jwt()` de `auth.ts` e em `lib/auth-token.ts`, removida depois de
   confirmar e corrigir): a leitura manual do cookie (`getToken()` em
   `lib/auth-token.ts`, usada fora do fluxo do Auth.js pra propagar o
   access token aos microsserviços) decidia o NOME do cookie
   (`__Secure-authjs.session-token` vs `authjs.session-token` sem
   prefixo) olhando `NODE_ENV`, mas o próprio Auth.js decide isso pelo
   protocolo REAL da requisição (`url.protocol === "https:"`, não
   `NODE_ENV` — confirmado lendo `@auth/core/src/lib/init.ts`). A imagem
   Docker tem `NODE_ENV=production` fixo (`Dockerfile`, `ENV
   NODE_ENV=production`), mas em dev local via `docker-compose` o
   protocolo continua http (só produção real, atrás do Cloudflare
   Tunnel — ADR-0019 — é https de verdade) — `getToken()` procurava o
   nome errado e sempre devolvia `null`, mesmo a sessão existindo de
   verdade (`auth()`, usado pelo `proxy.ts`/layout, decodificava o MESMO
   cookie sem problema, porque usa o protocolo real — a divergência era
   só entre os dois mecanismos de leitura do mesmo cookie). Corrigido
   tentando os dois nomes de cookie em `obterTokenBruto()`, em vez de
   inferir por variável de ambiente — funciona em dev, atrás do
   Cloudflare Tunnel e em qualquer topologia futura, sem depender de
   header de proxy específico (`x-forwarded-proto` ou similar) que pode
   nem existir localmente.

Revalidado do zero depois dos dois fixes, sempre contra o container
real (`docker compose up -d --build`, sete serviços de aplicação + toda
a infra): roteiro de login completo via `curl` + `GET /`, `/contas`,
`/transacoes`, `/cartoes`, `/documentos`, `/chat` autenticados — todos
200, dashboard renderizou o nome do usuário e o "disponível pra gastar"
reais. No meio da validação, Kafka caiu sozinho com
`NodeExistsException` no Zookeeper (znode efêmero zumbi de uma sessão
anterior, sem relação com o `web`) — resolvido recriando os containers
`zookeeper`/`kafka`.

`npm test` (52), `npm run lint`, `npm run build` e `npm audit --omit=dev`
(0 vulnerabilidades) verdes; build da imagem Docker validado local
(`docker build`) e via `docker compose up -d --build` contra a stack
real inteira.

Criado: job `web` em `.github/workflows/ci.yml`; serviço `web` em
`docker-compose.yml`. Alterado: `services/web/auth.ts` (discovery OIDC
desligado, `token`/`userinfo`/`jwks_endpoint` explícitos,
`AUTH_KEYCLOAK_INTERNAL_ISSUER` novo), `services/web/lib/auth-token.ts`
(`obterTokenBruto()` tenta os dois nomes de cookie; chamada de refresh
usa o issuer interno), `services/web/.env.example` (variável nova,
comentário reescrito), `.env.example` raiz (`AUTH_SECRET`),
`docs/architecture/diagrams.md` (container graph — `web`→`BudgetSvc`,
label com porta), `docs/architecture/overview.md` (tabela de clientes),
`docs/roadmap.md` (fatia 6 → Entregue), `docs/tasks.md` (item 10 →
concluído), `README.md` raiz.

## 2026-08-11 — Teste real pelo navegador: 2 bugs de UX corrigidos + achado de contenção do Ollama

Pedido: "preciso que você consiga acessar meu navegador para fazer testes
e resolver os possíveis bugs do nosso sistema" — usuário estava testando
o upload da fatura Santander (`Fatura_082026_teste_...pdf`) de verdade
contra o `web` recém fechado (fatia 6, item 10) e caiu num erro genérico
do Chrome ("This page couldn't load"). Autorizei acesso via
`claude-in-chrome` e testei ao vivo, no mesmo navegador do usuário (aba
nova, mas cookies compartilhados — mesmo perfil do Chrome).

**Bug 1 — sessão expirada quebrava a página em vez de avisar.**
Reproduzido na hora (aba nova herdou cookie de sessão velho, com token e
refresh token já expirados de tanto eu testar antes). Causa: o
`app/(app)/layout.tsx` já checava `sessao.error` (setado em `auth.ts`
quando a renovação falha) e mostrava um aviso, mas continuava
renderizando `children` por baixo — as páginas chamavam os
microsserviços com token morto, tomavam 401 não tratado, e o Next.js
abortava a conexão no meio do streaming (sem `error.tsx`/
`global-error.tsx` nenhum no projeto até então, então não tinha rede de
segurança nenhuma). Corrigido: `AppLayout` agora CORTA o render de
`children` quando `sessao.error` existe, mostrando só a tela "Sua sessão
expirou" com botão "Entrar novamente" — mesmo padrão de guarda que
`/login` já usa pro caso inverso. Adicionado `app/error.tsx` (boundary
de rota) e `app/global-error.tsx` (boundary do layout raiz) como rede de
segurança pra qualquer outro erro não tratado — nenhum dos dois existia
antes. Validado via `curl` com um cookie de sessão real e velho: antes
`GET /` dava 500 sem corpo útil; depois, 200 com a tela de "sessão
expirada" de verdade.

**Bug 2 — erro do chat aparecia como "Minified React error #441"
pro usuário.** Descoberto testando o chat de propósito enquanto a fatura
processava (pergunta do usuário: "a IA consegue responder enquanto
processa fatura?"). Mandei uma pergunta real no chat durante o
processamento — demorou ~127s e falhou (achado 3, abaixo), mas o erro
que apareceu na conversa foi um texto de framework sem sentido nenhum
pro usuário. Causa raiz: Server Action que **lança** (`throw`) uma
exceção tem a mensagem REDACTADA pelo Next.js em produção (troca por um
"Minified React error #441" genérico, sem digest nem pista nenhuma) —
mesmo com try/catch do lado do client, `.message` já chega sanitizado.
`enviarMensagemAction` (`app/(app)/chat/actions.ts`) era a única Server
Action do chat que deixava isso acontecer — `definirConfiguracaoIaAction`,
no mesmo arquivo, já usava o padrão certo (captura o erro e RETORNA um
objeto serializável, nunca lança). Corrigido replicando esse padrão:
`enviarMensagemAction` agora retorna
`{sucesso: true, resultado} | {sucesso: false, erro}`, nunca lança;
`chat-client.tsx` ajustado pro novo formato. Validado via `npm test`
(52)/lint/build limpos; reteste real do fluxo de timeout fica pendente
(ver achado 3).

**Achado 3 — Ollama só processa uma geração por vez (`ollama ps`
mostrou 1 slot ativo), então chat e extração de fatura NUNCA rodam em
paralelo nessa infra — competem pelo mesmo recurso.** Ao mandar uma
pergunta no chat enquanto a fatura Santander processava, a pergunta
ficou "na fila" atrás da extração, e como o `@Timeout` do `ai-service`
pro Ollama é bem mais curto (120s) que o do `document-service` (600s), o
chat estourou timeout primeiro (confirmado no log:
`OllamaRestClient#gerar timed out` às 14:43:41, ~127s depois do envio).
Pior: isso também **atrasou a própria extração da fatura**, que enfileirou
atrás da chamada do chat — a fatura, que já estava perto do limite de
10 minutos, estourou o timeout dela também nas duas tentativas feitas
nesta sessão (`ERRO_PROCESSAMENTO`, 2026-08-11T13:57 e 14:38 UTC,
exatamente ~10min01s de duração as duas vezes). Isso confirma, na
prática, o risco que já estava anotado desde a fatia 3
(`docs/tasks.md`, "inferência local em CPU pode levar vários minutos,
revisitar quando a fatia 6 chegasse") — só que o efeito colateral de
CONTENÇÃO entre features diferentes (chat vs. extração de documento,
todas usando o mesmo container `ollama` com 1 slot) não tinha sido
observado antes. **Não resolvido ainda** — usuário decidiu deixar o
reenvio da fatura de lado por ora; fica registrado como risco conhecido,
não mascarado. Opções futuras, não decididas: aumentar `@Timeout`/
`read-timeout` dos dois serviços, configurar `OLLAMA_NUM_PARALLEL` (efeito
duvidoso num host sem GPU — dividir a mesma CPU entre duas gerações
tende só a deixar as duas mais lentas, não resolver a contenção de
verdade), ou aceitar como limitação conhecida de ambiente de
desenvolvimento single-machine (produção real ainda não foi dimensionada
pra isso).

Criado: `services/web/app/error.tsx`, `services/web/app/global-error.tsx`.
Alterado: `services/web/app/(app)/layout.tsx` (corta `children` quando
`sessao.error`), `services/web/app/(app)/chat/actions.ts`
(`enviarMensagemAction` retorna em vez de lançar),
`services/web/app/(app)/chat/chat-client.tsx` (novo formato de retorno).
`npm test` (52)/lint/build limpos; validado contra o container `web`
real (rebuild + `curl` com sessão expirada real).

## 2026-08-11 — Ollama concorrente (achado de contenção real) + placa de vídeo pro servidor

Pedido: "preciso que o ollama seja capaz de processar várias faturas ao
mesmo tempo... e tem que ser capaz de responder vários usuários ao mesmo
tempo" — reação direta ao achado 3 da entrada anterior (chat e extração
de fatura brigando pelo mesmo slot do Ollama).

Testado (`docker-compose.yml`, mudança feita mas **ainda não commitada**):
`OLLAMA_NUM_PARALLEL=2` no serviço `ollama`. Validado com upload real de
fatura (Nubank, via `curl` direto nos serviços — descobri no caminho que
faltava o campo `tipo=FATURA_CARTAO` no multipart, spec já documentava
isso certo) + pergunta real no chat, as duas ao mesmo tempo. Resultado:
**ajuda, mas não resolve de verdade** — a primeira chamada do
`ai-service` ao Ollama rodou livre (~23s, sem contenção, terminou antes
do documento começar a gerar), mas a segunda chamada colidiu com o
prompt processing do documento (3139 tokens, ~129s só nessa fase) e
ficou esfaimada mesmo tendo slot dedicado — achado técnico real: o
`llama.cpp`/Ollama processa os slots dentro do MESMO laço de lote, e um
prompt grande domina esse laço, atrasando a geração de outro slot mesmo
ele "não estando na fila" formalmente. `NUM_PARALLEL=2` também dobrou o
consumo de RAM do Ollama (5,3GB → 7,87GB) só pelo cache de contexto
extra.

Medido o teto real da máquina: 15GB RAM total, só ~4GB livres com **só**
esse projeto rodando (nada de outros projetos). Servidor de produção tem
o mesmo perfil de hardware (`docs/architecture/deployment.md`) e **ainda
por cima já hospeda outros projetos** (portfólio, Umami, Postgres,
pgAdmin, Portainer, Watchtower) — rodar uma segunda instância completa
do Ollama (mais um modelo de ~5-8GB carregado) não cabe com folga em
nenhum dos dois ambientes. Concluído junto com o usuário: só ajuste de
config não resolve isso em CPU sem GPU; o caminho real é hardware.

**Decisão**: usuário tem uma RTX 5070 Ti (16GB GDDR7, 896 GB/s) numa
outra máquina e vai mover pro servidor de produção. Recomendação dada:
uma instância só do Ollama com GPU + `NUM_PARALLEL` mais alto (16GB de
VRAM dá folga — não precisa de duas instâncias separadas, GPU lida bem
com lotes concorrentes, diferente da CPU). Pesquisei GPUs de mercado
(2026) antes de confirmar que a placa que ele já tinha servia — RTX 5070
Ti tem banda de memória (896 GB/s) equivalente ou melhor que as opções
de orçamento pesquisadas.

**Configuração de acesso SSH pro Claude Code**, pedida explicitamente
pelo usuário pra eu poder levantar informação do servidor antes da
instalação: recusei digitar a senha SSH fornecida em chat (regra de
segurança que não abre exceção mesmo com autorização explícita — nunca
entro credencial real em nenhum prompt). Gerada chave dedicada
(`ed25519`, sem passphrase, só pra esse fim — nome
`id_ed25519_wepdev_financas_servidor`, fora do repositório, com alias em
`~/.ssh/config` local), usuário adicionou a chave pública no
`authorized_keys` do servidor com restrição `from="<faixa da rede
Wi-Fi>"` (só autentica vindo de dentro dessa rede, mesmo que a chave
pública vaze). **Achado real no meio da configuração**: primeira
tentativa de conexão falhou — o cliente SSH nem chegou a oferecer a
chave, porque `/etc/ssh/sshd_config` tinha `AuthenticationMethods
password` forçando só senha, apesar de `PubkeyAuthentication yes`
também estar ligado. Corrigido comentando essa linha (backup do config
original feito antes, no próprio servidor) + `sudo systemctl reload
ssh` (não `sshd` — nome do serviço no Ubuntu é `ssh.service`, achado
raso mas travou por um passo). Conexão validada sem senha depois disso.

Levantamento do servidor real, pela chave nova: Intel i7-8700 (6
núcleos/12 threads), 15GB RAM (só 2,1GB em uso agora, produção ainda não
roda o `wepdev-financas`), 140GB de disco livre, Ubuntu 24.04.4/kernel
6.8, só GPU integrada Intel UHD 630 (nenhum driver NVIDIA ainda — terreno
limpo), Docker 29.2.1/Compose v5.0.2 (moderno, suporta reserva de GPU em
compose sem drama). Usuário confirmou fisicamente: espaço no gabinete,
conector de energia certo na fonte, fonte de 850W (folga real acima dos
~300W da placa). Nenhum bloqueio encontrado pra instalação.

**Pendente pra próxima sessão**: usuário ainda não instalou a placa
fisicamente. Depois de instalada: driver NVIDIA + NVIDIA Container
Toolkit no servidor, config de GPU no `docker-compose.yml` (serviço
`ollama`, ainda não escrita), decidir destino do `OLLAMA_NUM_PARALLEL=2`
(hoje só ajuda parcialmente em CPU — com GPU pode subir mais), e
documentar a decisão final como ADR (referenciado como ADR-0029 no
comentário do `docker-compose.yml`, ainda não escrito de verdade). Nada
disso foi commitado ainda — ver estado do repo na entrada anterior.

Criado: chave SSH `~/.ssh/id_ed25519_wepdev_financas_servidor` (fora do
repo), entrada em `~/.ssh/config` (fora do repo), backup
`/etc/ssh/sshd_config.bak-<data>` no servidor. Alterado:
`docs/architecture/security.md` (inventário de credenciais — chave SSH
nova), `docker-compose.yml` (`OLLAMA_NUM_PARALLEL=2`, ainda sem ADR),
`/etc/ssh/sshd_config` no servidor (`AuthenticationMethods` comentado).

## 2026-08-11 (continuação) — GPU instalada, Ollama migrado pro servidor (ADR-0029)

Pedido: comparar placas de vídeo mais baratas (usuário pesquisando preço
antes de comprar), depois decisão final de usar a RTX 5070 Ti que já
tinha disponível numa outra máquina, seguida de "faça todas as
configurações necessárias" pra colocar o Ollama do servidor em uso de
verdade no projeto.

**Comparação de GPUs** (só pesquisa, sem código): RTX 3060 12GB, RTX
2060 12GB, RTX 2080 Ti 11GB (achado: banda de memória, 616 GB/s, maior
que a RTX 5060 Ti nova — melhor custo-benefício de toda a lista), RTX
4060 12GB (não existe — só RTX 4060 Ti em 8/16GB ou RTX 4070 em 12GB),
RTX 4060 Ti 16GB (achado real: banda de memória, 288 GB/s, **menor** que
a RTX 3060 de 2021, apesar de mais nova e com mais VRAM — evitar
recomendar), comparativo final de todas as placas 12GB+ discutidas.
Usuário decidiu manter a RTX 5070 Ti que já tinha.

**Instalação física**: usuário identificou que o SSD de sistema
(XPG SPECTRIX, numa placa adaptadora PCIe) ocupava o slot que a GPU
precisava. Descoberto o modelo da placa-mãe sem precisar de sudo
(`/sys/class/dmi/id/board_name`, legível sem root) — **ASUS TUF
B360M-PLUS GAMING/BR** — confirmado no manual oficial que ela tem 2
slots M.2-2280 nativos. Antes de autorizar a troca física, verifiquei
`/etc/fstab` e `efibootmgr -v`: todas as montagens usam UUID/LVM (nunca
caminho físico de dispositivo), e a entrada de boot UEFI ativa também
usa UUID da partição GPT — confirmação de que trocar o disco de slot
físico não quebraria o boot. Usuário desligou (`sudo shutdown -h now` —
minha chave SSH não tinha `sudo` ainda nessa hora, precisou pedir pra
ele rodar), moveu o SSD pro M.2 nativo, instalou a GPU, religou.
Verificado por SSH: sistema íntegro, todos os outros projetos do
servidor (portfólio, Umami, Postgres, pgAdmin, Portainer, Watchtower)
voltaram sozinhos, GPU detectada no barramento PCIe.

**Sudo irrestrito pro Claude Code**: usuário pediu acesso de sudo pra eu
rodar os comandos direto, sem ficar repassando. Apresentei o trade-off
(escopo restrito aos comandos da sessão vs. total) — usuário escolheu
total. `wepdev ALL=(ALL) NOPASSWD: ALL` em `/etc/sudoers.d/claude-code`
(permissão 440, validado com `visudo -c` antes de confiar). Documentado
em `security.md` como atualização da entrada da chave SSH, com o
trade-off de segurança explícito (rede continua sendo a única barreira
se a chave privada vazar).

**Driver NVIDIA + Container Toolkit**: pedido `nvidia-driver-570-open`
(mínimo confirmado por pesquisa pra suportar Blackwell/RTX 50), mas o
`apt` resolveu pra `580.173.02` (mais novo, também suporta — sem
problema). **Achado real no meio do processo**: `sudo apt update`
travou de verdade (~8 minutos sem uso de CPU, conectividade de rede/DNS/
mirror todas OK — provável prompt interativo escondido); resolvido com
Ctrl+C do lado do usuário + `kill -9` de contingência (não precisou).
Depois de reiniciar, driver carregou limpo: `nouveau` sumiu, RTX 5070 Ti
reconhecida (`nvidia-smi`: 580.173.02, CUDA 13.0, 16303MiB). NVIDIA
Container Toolkit instalado e configurado (`nvidia-ctk runtime configure
--runtime=docker`), testado com container `nvidia/cuda` real reconhecendo
a placa via `--gpus all`.

**Ollama no servidor, testado de verdade**: container `ollama/ollama`
avulso (fora de qualquer `docker-compose.yml` por ora), `--gpus all`,
`OLLAMA_NUM_PARALLEL=4`, porta `11434` na rede local, modelo `llama3.1`
baixado. Resultados medidos, não estimados:
- Velocidade: segunda chamada (modelo já quente) gerou 81 tokens em
  638ms = **~127 tokens/segundo** — mais de 30x mais rápido que os ~4
  tokens/s da CPU.
- Concorrência real (o teste que motivou tudo isso): prompt grande
  (~3000 tokens, simulando fatura) + pergunta curta enviados ao mesmo
  tempo — **os dois terminaram em ~4 segundos cada**, sem fila, sem
  timeout. Comparado com o mesmo cenário na CPU (chat esperou 127s e
  estourou timeout de 120s), confirma que o problema está resolvido de
  verdade, não só mitigado.

**Projeto reconfigurado pra usar o Ollama do servidor**:
`docker-compose.yml` — serviço `ollama` local removido por completo
(container, porta 11500, volume `ollama-data`), `document-service`/
`ai-service` ganharam `OLLAMA_BASE_URL: ${OLLAMA_SERVER_URL}` nos
`depends_on`/`environment` (removida a dependência de `ollama:
service_started`, que não existe mais). `OLLAMA_SERVER_URL` é variável
nova, só em `.env` local (gitignored) — endereço de rede interna nunca
em arquivo versionado, mesma regra de sempre; documentado com
placeholder em `.env.example`, incluindo como voltar a apontar pra um
Ollama local se a rede não estiver disponível. Validado de ponta a
ponta: `document-service`/`ai-service` recriados localmente, pergunta
real via `POST /api/v1/chat` batendo no Ollama do servidor pela rede,
resposta em 2,2 segundos.

ADR-0029 escrita (`docs/architecture/adr/0029-ollama-servidor-gpu-dedicada.md`)
consolidando toda a decisão, os achados técnicos e os trade-offs
assumidos (dependência de rede, sudo irrestrito, porta do Ollama exposta
na LAN sem autenticação própria). `docs/architecture/deployment.md`
atualizado com o estado real do servidor pós-GPU (placa-mãe, driver,
Ollama). `README.md`/`diagrams.md` atualizados (Ollama não é mais
"local"). Pendência registrada na própria ADR: o `docker run` avulso do
Ollama no servidor deve ser incorporado a um `docker-compose.yml` de
produção quando a fatia 9 (deploy completo, ainda `🔲 Planejado`)
acontecer — hoje é configuração solta, não gerenciada pelo projeto.

Criado: `docs/architecture/adr/0029-ollama-servidor-gpu-dedicada.md`;
`.env` (local, gitignored); `/etc/sudoers.d/claude-code` no servidor;
container `ollama` no servidor (fora de compose). Alterado:
`docker-compose.yml` (remove serviço `ollama` + volume `ollama-data`,
`document-service`/`ai-service` apontam pro servidor via
`OLLAMA_SERVER_URL`), `.env.example` (variável nova documentada),
`docs/architecture/security.md` (sudo irrestrito documentado),
`docs/architecture/deployment.md` (estado real do servidor pós-GPU),
`README.md`, `docs/architecture/diagrams.md`. No servidor:
`/etc/modprobe.d`/initramfs (driver NVIDIA), `/etc/docker/daemon.json`
(runtime NVIDIA).
