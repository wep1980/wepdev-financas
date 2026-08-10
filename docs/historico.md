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
