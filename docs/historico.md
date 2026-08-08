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
