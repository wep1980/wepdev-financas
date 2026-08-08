# Tasks — Fatia Atual

> Backlog de trabalho em detalhe, só da fatia em andamento (roadmap #1). Ao
> concluir a fatia, arquive as tasks completas (ou simplesmente comece um
> bloco novo pra próxima fatia) e não deixe esse arquivo virar um histórico
> gigante — histórico de decisão fica em ADR/git, não aqui.

## Fatia 1 — Contas + Transações (back-end)

Specs já existem: `docs/specs/account-service.yaml`,
`docs/specs/transaction-service.yaml`. `account-service` tem **CRUD
completo** (criar/listar/buscar/atualizar/excluir) + ajuste de saldo
(débito/crédito) funcionando ponta a ponta, com teste. `transaction-service`
tem **registrar**, **listar**, **cancelar**, **editar transação**,
**resumo por categoria** e **transações recorrentes** (com job agendado)
funcionando ponta a ponta contra o `account-service` real, inclusive via
`docker compose up` com autenticação de verdade (issuer do Keycloak
corrigido) — 2026-08-07/08. CI 100% verde (`NVD_API_KEY` ativa desde
2026-08-08, 2 CVEs reais corrigidas por bump de versão). Backlog do
`transaction-service` completo — próxima fatia é `card-service` (ver
seção "Próxima fatia" no fim deste arquivo).

### `account-service`

- [x] Scaffold do projeto Quarkus (Java 21), estrutura de pacotes por camada
      (`domain` / `application` / `infrastructure` — `domain` não conhece
      Panache/JAX-RS/Kafka, só interfaces/portas).
- [x] Entidade `Conta` (domínio) com regra: saldo, tipo
      (`CORRENTE|POUPANCA|CARTEIRA|CARTAO_CREDITO|INVESTIMENTO`), exclusão
      lógica (`ativa`), `debitar`/`creditar` guardando a regra de saldo
      insuficiente.
- [x] Caso de uso: **criar conta** — `CriarContaUseCase`, com teste unitário
      (regra + mocks dos ports).
- [x] Casos de uso: listar contas ativas do usuário autenticado
      (`ListarContasUseCase`), buscar conta por id (`BuscarContaUseCase`),
      atualizar nome/instituição (`AtualizarContaUseCase`), excluir/inativar
      (`ExcluirContaUseCase`) — os quatro lançam `ContaNaoEncontradaException`
      → 404 tanto pra id inexistente quanto pra conta de outro usuário
      (mesmo erro nos dois casos, de propósito — evita IDOR).
- [x] Casos de uso: debitar saldo (`DebitarSaldoUseCase`), creditar saldo
      (`CreditarSaldoUseCase`) — reusados pelo `transaction-service` via
      endpoint interno.
- [x] Domínio: `Conta.atualizar(nome, instituicao)` — só esses dois campos
      são editáveis; `tipo`/`saldo`/`usuarioId` não (saldo só muda via
      débito/crédito, dono não é transferível).
- [x] Endpoint REST `POST /api/v1/contas` conforme
      `docs/specs/account-service.yaml`, com Bean Validation.
- [x] Endpoint REST `GET /api/v1/contas` (contas do usuário autenticado) e
      `GET /api/v1/contas/{id}`.
- [x] Endpoint REST `PUT /api/v1/contas/{id}` (atualizar nome/instituição) e
      `DELETE /api/v1/contas/{id}` (exclusão lógica via `inativar()`,
      idempotente, 204).
- [x] **Segurança — `usuarioId` nunca vem do cliente.** `CriarContaRequest`
      não tem mais campo `usuarioId`; todo caso de uso que lê/muta conta
      recebe o `usuarioId` extraído do claim `sub` do token JWT
      (`ContaResource.usuarioIdAutenticado()`, via `SecurityIdentity` +
      `JsonWebToken`), nunca de parâmetro de request. Fecha uma falha real:
      antes dava pra criar conta em nome de outro usuário, listar/buscar
      conta de qualquer um só sabendo o id (IDOR). Decisão tomada com o
      usuário (2026-08-07) — trade-off era mudar contrato já validado no
      Postman, optamos por corrigir de vez.
- [x] Endpoints internos `POST /api/v1/contas/{id}/debitos` e
      `POST /api/v1/contas/{id}/creditos` (role `service`, ADR-0003) —
      especificados em `account-service.yaml` e implementados. Sem
      idempotência (retry do `transaction-service` pode duplicar ajuste em
      caso de falha após aplicar e antes de responder) — risco aceito por
      ora, revisar quando `transaction-service` implementar o retry de
      verdade (SmallRye Fault Tolerance).
- [x] Segurança: `POST`/`GET`/`PUT`/`DELETE /contas*` exigem role `usuario`;
      `POST /contas/{id}/debitos` e `/creditos` exigem role `service`
      (ADR-0003).
- [x] Tratamento de exceção estruturado: `ConstraintViolationExceptionMapper`
      (400, corpo com campo+mensagem por violação — cobre "campo
      obrigatório" e demais regras de Bean Validation),
      `ContaNaoEncontradaExceptionMapper` (404, corpo com mensagem),
      `SaldoInsuficienteExceptionMapper` (422, corpo com mensagem). DTOs
      `ErroResponse`/`ErroValidacaoResponse`.
- [x] Publicação de evento Kafka `conta.eventos` em criação (`ContaCriadaEvento`).
- [x] Migração Flyway (`V1__create_contas_table.sql`) — schema por migração
      versionada, não geração automática do Hibernate.
- [x] Testes: `ContaTest` (domínio, 9 casos, inclui `atualizar()`),
      `CriarContaUseCaseTest`, `ListarContasUseCaseTest`,
      `BuscarContaUseCaseTest`, `AtualizarContaUseCaseTest`,
      `ExcluirContaUseCaseTest`, `DebitarSaldoUseCaseTest`,
      `CreditarSaldoUseCaseTest` (unitários com Mockito, cobrindo sucesso +
      não encontrada + conta de outro usuário), `ContaResourceTest`
      (`@QuarkusTest`, MySQL/Kafka via Dev Services automático,
      `@TestSecurity`+`@JwtSecurity`/`@Claim` pra simular `sub` do token —
      cobre role `service` x `usuario`, dono x não-dono, 400/404/422).
      45 testes, todos passando.
- [x] `docker-compose.yml`: `build.dockerfile` do `account-service` aponta
      pra `src/main/docker/Dockerfile.jvm` (gerado pelo Quarkus). Validado
      com `docker build` isolado (`mvn package` antes, já que essa imagem
      não faz build multi-stage — só copia `target/quarkus-app/`); falta
      ainda validar `docker compose up -d` de ponta a ponta, mas isso só faz
      sentido depois que `transaction-service` também tiver Dockerfile.
- [x] README do serviço (`services/account-service/README.md`).

### `transaction-service`

- [x] Scaffold do projeto Quarkus (Java 21), mesma estrutura de camadas do
      `account-service` — extensions extras: `rest-client-jackson`
      (chamar o account-service), `rest-client-oidc-filter` (token de
      serviço automático), `smallrye-jwt` (extrair `sub` do token, mesmo
      padrão do `account-service`).
- [x] Entidade `Transacao` (domínio): tipo (`RECEITA|DESPESA`), status
      (`PENDENTE|CONFIRMADA|CANCELADA` — nasce sempre `CONFIRMADA`, porque
      quando `Transacao.criar()` roda o efeito no saldo já aconteceu).
- [x] Caso de uso: **registrar transação** (`RegistrarTransacaoUseCase`) →
      chama `account-service` de forma síncrona (débito/crédito) antes de
      persistir — se falhar, nada é salvo (sem transação "fantasma",
      testado). Retry/timeout via SmallRye Fault Tolerance
      (`@Retry`/`@Timeout`) nos métodos do REST client.
      `usuarioId` extraído do token (mesmo padrão do `account-service`,
      não vem do corpo da requisição).
- [x] **Duas chamadas ao account-service, dois tokens diferentes** (ver
      `services/transaction-service/README.md`): `GET /contas/{id}` com o
      **token do próprio usuário repassado** (`PropagarAutorizacaoHeadersFactory`,
      um `ClientHeadersFactory`) — reusa o 404 do account-service pra
      confirmar posse da conta, sem reimplementar a checagem aqui; depois
      `POST /contas/{id}/debitos` ou `/creditos` com **token de serviço**
      (client_credentials automático via `quarkus-rest-client-oidc-filter`,
      client `transaction-service`, role `service`). Validado com os dois
      serviços rodando de verdade (não só mock): saldo debitado
      corretamente, 422 sem persistir nada quando saldo insuficiente, 404
      quando a conta é de outro usuário.
- [x] Endpoint REST `POST /api/v1/transacoes` conforme
      `docs/specs/transaction-service.yaml`, com Bean Validation e os
      mesmos exception mappers estruturados do `account-service`
      (`ConstraintViolationExceptionMapper`, `ContaNaoEncontradaExceptionMapper`
      404, `SaldoInsuficienteExceptionMapper` 422).
- [x] Publicação de evento Kafka `transacao.eventos`
      (`TransacaoRegistradaEvento`) — validado consumindo de verdade via
      `kafka-console-consumer`.
- [x] Migração Flyway (`V1__create_transacoes_table.sql`).
- [x] `docker-compose.yml`: `build.dockerfile` aponta pra
      `Dockerfile.jvm`, mesmo padrão do `account-service` — validado com
      `docker build` isolado.
- [x] README do serviço (`services/transaction-service/README.md`).
- [x] Testes: `TransacaoTest` (domínio, 4 casos),
      `RegistrarTransacaoUseCaseTest` (unitário com Mockito — cobre
      sucesso débito/crédito e "não salva nada quando account-service
      falha"), `TransacaoResourceTest` (`@QuarkusTest`, port
      `AccountServiceClient` substituído por mock via `QuarkusMock` —
      permite testar 201/400/404/422 sem precisar do account-service real
      no ar).
- [x] Caso de uso: **listar transações** (`ListarTransacoesUseCase`) —
      `GET /api/v1/transacoes`, sempre do usuário autenticado (token),
      filtros opcionais `contaId`/`inicio`/`fim` via query param
      (`TransacaoFiltro`). Ordenado por data desc.
- [x] **Bug real encontrado e corrigido rodando via `docker compose`** (não
      aparecia em `quarkus:dev` nem nos testes automatizados): o Keycloak
      (`start-dev`, sem hostname fixo) infere o `issuer` do token pelo Host
      da requisição — de dentro de um container isso vira `keycloak:8080`,
      mas todo token pego via acesso externo (Postman, navegador) usa
      `localhost:8080`. Os dois serviços rejeitavam qualquer token com 401
      quando rodando via `docker compose up`. Corrigido: `KC_HOSTNAME=localhost`
      fixo no Keycloak (issuer estável não importa de onde o token foi
      pedido) + `quarkus.oidc.discovery-enabled=false` com
      `jwks-path`/`token-path` manuais nos dois serviços (senão o
      discovery document passa a devolver URLs absolutas em "localhost",
      inalcançáveis de dentro de um container) + `quarkus.oidc.token.issuer`
      explícito. Afeta também o `oidc-client` de saída do
      `transaction-service` (mesma correção). Validado de ponta a ponta
      via containers reais: criar conta (saldo 300) → registrar despesa de
      50 → saldo vira 250, com token pego via `localhost:8080` e as duas
      chamadas internas (usuário + serviço) passando pela rede Docker.
- [x] Caso de uso: **cancelar transação** (`CancelarTransacaoUseCase`,
      `DELETE /api/v1/transacoes/{id}`) — exclusão lógica (`CANCELADA`),
      reverte o efeito no saldo de forma síncrona com o `account-service`
      (`DESPESA` credita de volta, `RECEITA` debita de volta — se a
      reversão de uma `RECEITA` falhar por saldo insuficiente, 422 e a
      transação continua `CONFIRMADA`, não fica em estado inconsistente).
      Idempotente: cancelar de novo uma transação já cancelada é 204 sem
      reverter o saldo outra vez (`Transacao.isCancelada()` checado antes
      de chamar o account-service). 404 pra id inexistente ou de outro
      usuário (mesmo padrão anti-IDOR — `TransacaoNaoEncontradaException`).
      `TransacaoRepository.salvar()` virou upsert (antes só inseria) pra
      suportar esse update. Validado de ponta a ponta via containers
      reais: despesa de 50 numa conta de 300 → saldo 250 → cancela → saldo
      volta a 300 → cancela de novo → continua 300 (não reverte duas vezes).
- [x] Testes: `TransacaoTest` (domínio, 5 casos, inclui `cancelar()`),
      `RegistrarTransacaoUseCaseTest`, `ListarTransacoesUseCaseTest`,
      `CancelarTransacaoUseCaseTest` (unitários com Mockito),
      `TransacaoResourceTest` (`@QuarkusTest`, cobre 201/400/404/422,
      listagem com filtro/isolamento por usuário, cancelamento com
      reversão/idempotência/dono). 28 testes, todos passando.
- [x] Caso de uso: **editar transação** (`AtualizarTransacaoUseCase`,
      `PUT /api/v1/transacoes/{id}`) — descricao/valor/categoria/dataTransacao
      editáveis; `contaId`/`tipo`/`usuarioId` não (trocar de conta ou tipo é
      cancelar e recriar, evita ambiguidade de reversão entre contas
      diferentes). Se o valor mudou, ajusta o saldo pela DIFERENÇA (delta)
      numa chamada só ao `account-service` (não reverte-e-reaplica em duas
      chamadas — evita janela de inconsistência se a segunda falhasse). 422
      se a transação já estava `CANCELADA` (`TransacaoCanceladaException`,
      nova). 404 pra id inexistente ou de outro usuário (mesmo padrão
      anti-IDOR). Validado de ponta a ponta via containers reais: despesa de
      100 numa conta de 1000 → editar pra 150 (debita +50, saldo 850) →
      editar pra 60 (credita 90, saldo 940) → cancelar → editar de novo dá
      422. 9 testes unitários (`AtualizarTransacaoUseCaseTest`) + 7 de
      integração (`TransacaoResourceTest`), todos passando (47 no total do
      serviço, sem regressão no `account-service`).
- [x] Caso de uso: **resumo por categoria** (`ResumoPorCategoriaUseCase`,
      `GET /transacoes/resumo-por-categoria?inicio&fim`) — agregação usada
      pelo dashboard (fatia 6) e pela tool de IA (fatia 5); implementado uma
      vez, os dois consumidores chamam o mesmo endpoint. Só soma `DESPESA`
      `CONFIRMADA` (ignora `RECEITA` e transação `CANCELADA`); transação sem
      categoria agrupa em "Sem categoria"; `percentualDoTotal` calculado
      sobre o total gasto do período; `totalGastoPeriodoAnterior` compara com
      o período imediatamente anterior de mesma duração (nulo se a categoria
      não teve gasto nele); ordenado por `totalGasto` decrescente, empate
      desempatado por nome da categoria (achado num teste: `HashMap` interno
      do `groupingBy` não garante ordem determinística entre categorias com
      total igual). 400 se `inicio`/`fim` ausente ou `inicio` depois de `fim`
      (`IntervaloInvalidoException`, nova). 7 testes unitários
      (`ResumoPorCategoriaUseCaseTest`) + 5 de integração
      (`TransacaoResourceTest`), todos passando (59 no total do serviço).
      Validado de ponta a ponta via containers reais: 150 de Alimentação +
      50 de Transporte + 3000 de Receita (ignorada) → resumo retorna
      Alimentação 75%, Transporte 25%.
- [ ] Testes de integração com Testcontainers cobrindo o `account-service`
      real (Docker), não só o mock via `QuarkusMock` — útil pra pegar
      divergência de contrato entre os dois serviços que o mock não pegaria.

### Transações recorrentes (`transaction-service`)

Conforme `docs/specs/transaction-service.yaml` (endpoints
`/api/v1/transacoes-recorrentes*`) e ADR-0009 — pré-requisito pra fatia 5
(`ai-service` chamar `criar_transacao` com recorrência, PRD 3.5).

- [x] Entidade `TransacaoRecorrente` (domínio): frequência (só `MENSAL` no
      v1, `proximaDataVencimento()` calcula via `dataInicio.plusMonths(ocorrenciasGeradas)`),
      `quantidadeOcorrencias` nullable (null = indefinida),
      `ocorrenciasGeradas`, status (`ATIVA|PAUSADA|CANCELADA|CONCLUIDA`).
      `registrarOcorrenciaGerada()` conclui automaticamente ao atingir o
      limite. Distinta de parcelamento de cartão (ADR-0009, `card-service`
      não reaproveita essa classe).
- [x] Caso de uso: **criar regra recorrente** (`CriarTransacaoRecorrenteUseCase`,
      `POST /transacoes-recorrentes`) → gera a 1ª ocorrência (`Transacao`
      com `transacaoRecorrenteId` preenchido) imediatamente, **reusando
      `RegistrarTransacaoUseCase`** (mesmo caminho síncrono com
      `account-service` de uma transação avulsa, sem duplicar lógica de
      saldo). Se `quantidadeOcorrencias == 1`, a regra já nasce `CONCLUIDA`.
- [x] **Job agendado** (`GerarOcorrenciasRecorrentesJob`, `quarkus-scheduler`,
      cron diário 00:05) que gera a próxima ocorrência de cada regra
      `ATIVA` vencida, respeitando `quantidadeOcorrencias` — ao atingir o
      limite, regra vira `CONCLUIDA` automaticamente. Núcleo testável
      separado do wrapper (`GerarOcorrenciasRecorrentesUseCase.executar(LocalDate hoje)`
      recebe a data como parâmetro em vez de ler o relógio — testado com
      datas controladas, sem `Thread.sleep`/tempo real). Gera no máximo 1
      ocorrência por regra por execução; atraso é recuperado
      incrementalmente nas execuções seguintes. Desabilitado em teste
      (`%test.quarkus.scheduler.enabled=false`) pra não rodar em paralelo
      com a suíte.
- [x] Caso de uso: **cancelar regra** (`CancelarTransacaoRecorrenteUseCase`,
      `DELETE /transacoes-recorrentes/{id}`, exclusão lógica, idempotente)
      — não afeta ocorrências já geradas.
- [x] Endpoints REST (`POST`/`GET`/`GET {id}`/`DELETE`) conforme o contrato,
      mais `GET /proximos-vencimentos` (role `service`, usado pelo futuro
      `notification-service`, ADR-0010) — todos em `TransacaoRecorrenteResource`.
- [x] Testes unitários: geração de ocorrência respeitando limite
      (`deveriaConcluirRegra_quandoAtingeQuantidadeOcorrenciasNestaExecucao`),
      regra indefinida nunca conclui sozinha (24 execuções seguidas,
      continua `ATIVA`), cancelamento não gera mais ocorrências (regra
      cancelada não aparece em `listarAtivas()`). 7 testes de domínio
      (`TransacaoRecorrenteTest`) + testes de caso de uso pra cada operação
      + 12 de integração REST (`TransacaoRecorrenteResourceTest`, inclui
      role `service` no endpoint interno) — 91 testes no total do serviço.
      Validado de ponta a ponta contra containers reais: criar regra
      indefinida → saldo reflete 1ª ocorrência; criar regra com
      `quantidadeOcorrencias=1` → nasce `CONCLUIDA`; cancelar → some da
      listagem `ATIVA`, idempotente; `/proximos-vencimentos` com token de
      usuário dá 403, com token de serviço (client_credentials) retorna a
      janela correta.
- [ ] Teste de integração (Testcontainers) cobrindo o **wrapper do
      scheduler** (`GerarOcorrenciasRecorrentesJob`) disparando via cron de
      verdade, não só o caso de uso chamado direto com data controlada —
      exigiria manipular o relógio do container ou esperar tempo real,
      deixado pra quando isso for realmente necessário (o núcleo da lógica
      já está coberto).

### Transversal

- [x] Confirmado: `docker compose up -d --build account-service
      transaction-service` sobe os dois do zero (`mvn package` antes em
      cada um), health check 200 nos dois via container — 2026-08-07.
- [x] README de `account-service` e `transaction-service`.
- [x] Fluxo completo validado manualmente via Postman/curl contra
      `quarkus:dev` real (não só `@QuarkusTest`): criar conta → listar →
      buscar por id → debitar (role `service`) → 403 debitando com role
      `usuario`. Duas correções de ambiente de dev encontradas nesse
      processo (ver `docs/postman/README.md` e commits): (1)
      `quarkus.observability.lgtm.enabled=false` em dev — sem
      `otel-collector` no compose, o Quarkus tentava provisionar um stack
      LGTM sozinho via Dev Services e travava o boot; (2) usuário de teste
      do Keycloak (`realm-financas.json`) precisava de `firstName`/
      `lastName` — Keycloak 25 exige esses campos no user profile por
      padrão, senão o grant de senha falha com "Account is not fully set
      up". Também adicionado ao realm um mapeamento de service account
      (`transaction-service` → role `service`) só pra permitir testar os
      endpoints internos antes do `transaction-service` existir de verdade.
- [x] **Repositório git inicializado** — `git init` + primeiro commit
      (152 arquivos, tudo que existia até aqui). Ainda sem remoto no
      GitHub (usuário decide quando conectar) — 2026-08-07.
- [x] `.github/workflows/ci.yml` (ADR-0018): job `changes` detecta path
      alterado (`dorny/paths-filter`), dispara `account-service` e/ou
      `transaction-service` só se o respectivo `services/*` mudou. Cada
      job: `mvn test` (unitário + integração) + cobertura JaCoCo publicada
      como artefato do run + build da imagem Docker (validação, não
      publica em registry) + `mvn dependency-check:check` (ADR-0017, scan
      OWASP). CD fica pra fatia 9, como já decidido.
- [x] **Cobertura de testes (JaCoCo)** — `jacoco-maven-plugin` (0.8.15,
      confirmado via Maven Central) nos dois `pom.xml`, `prepare-agent`
      (injeta o javaagent via `@{argLine}` no surefire — account-service
      não tinha essa property configurada, precisou adicionar) + `report`
      bindado na fase `test`, então roda dentro do `mvn test` normal
      (validado: gera HTML com contagem real de classes analisadas —
      63 em transaction-service, 30 em account-service). CI publica o
      relatório HTML como artefato do workflow run (`actions/upload-artifact`,
      14 dias de retenção) — sem Sonar/serviço externo por enquanto
      (avaliado SonarCloud, usuário preferiu não configurar agora). Além
      disso, `madrapps/jacoco-report@v1.8.0` (confirmado via GitHub
      Releases) posta um comentário com o resumo de cobertura direto na
      PR, lendo o `jacoco.xml` gerado junto com o HTML — só roda em
      `pull_request` (`if: github.event_name == 'pull_request'`, sem
      sentido em push direto pra `main`, não tem PR pra comentar). Precisou
      de `permissions: pull-requests: write` a nível de job (sobrescrevendo
      o `read` do topo do arquivo só pros jobs `account-service` e
      `transaction-service`) — ainda não validado contra uma PR real nesta
      sessão, só localmente (a lógica do `if:` é padrão bem estabelecido do
      GitHub Actions).
- [x] **Build de imagem Docker no CI (validação)** — `mvn package -DskipTests`
      + `docker build -f src/main/docker/Dockerfile.jvm` a cada job, só pra
      garantir que a imagem continua buildando a cada mudança; não publica
      em registry nem faz deploy (isso é fatia 9). Runners `ubuntu-latest`
      já vêm com Docker instalado, não precisou de setup extra.
- [x] `dependency-check-maven` (13.0.0 — confirmado via Maven Central,
      2026-08-07) declarado nos dois `pom.xml`, sem `<executions>` (não
      roda em `mvn test`/`package` local, só via `mvn dependency-check:check`
      explícito no CI — não pesa o loop de dev).
- [x] `.github/dependabot.yml`: Maven (os dois serviços), Docker (os dois
      Dockerfile), `docker-compose` (raiz), `github-actions` — atualização
      semanal.
- [x] **Achado rodando local**: `dependency-check-maven` falha com "Invalid
      API Key" sem `NVD_API_KEY` — a NVD passou a exigir chave (gratuita)
      pra sincronizar a base de CVE. Sem essa secret configurada no GitHub,
      o job de scan vai falhar em todo PR — **configurar
      `NVD_API_KEY` nos secrets do repositório antes do primeiro PR real**
      (gerar em https://nvd.nist.gov/developers/request-an-api-key).
      Documentado no inventário de credenciais (`security.md`).
- [x] **Repositório GitHub criado e conectado** —
      `github.com/wep1980/wepdev-financas` (privado), via GitHub CLI
      instalado sem admin (zip portátil, `.msi` pedia elevação que o
      ambiente não tinha) — 2026-08-07.
- [x] **CI validado rodando de verdade** no GitHub Actions (não só
      localmente): job `changes` disparou os dois serviços corretamente
      (path filter funcionou), `mvn test` **passou nos dois** — 45 +28
      testes rodando no runner hospedado, incluindo Dev Services
      (Testcontainers) pra MySQL/Kafka, sem configuração extra.
- [x] **Bug real #1 encontrado e corrigido**: primeiro run falhou com
      `./mvnw: Permission denied` (exit 126) — o Windows não rastreia bit
      de execução (`core.filemode=false`), então o `mvnw` foi commitado
      como `100644` em vez de `100755`. Corrigido com
      `git update-index --chmod=+x` nos dois `mvnw`.
- [x] **Bug real #2 encontrado e corrigido, contra PR real do Dependabot**:
      `dorny/paths-filter` falhava com "Resource not accessible by
      integration" em toda PR do Dependabot — PR de fonte externa recebe
      `GITHUB_TOKEN` com permissão mínima por padrão (proteção do GitHub
      contra supply-chain attack via bot). Corrigido com bloco
      `permissions: contents: read, pull-requests: read` explícito no
      workflow. Validado: as 9 PRs que o Dependabot abriu automaticamente
      (assim que `dependabot.yml` foi pro ar) passaram a rodar `changes` +
      `Testes` com sucesso depois da correção (`@dependabot rebase` pra
      forçar re-teste).
- [x] **`NVD_API_KEY` ativa e funcionando** — usuário gerou e ativou a
      chave (segunda tentativa; a primeira ficou presa na confirmação por
      e-mail, ver entrada de 2026-08-07). Configurada nos dois secret
      stores do GitHub (`gh secret set --app actions` e `--app dependabot`)
      — 2026-08-08. Primeira execução completa do scan revelou 2 CVEs reais
      (não mais "Invalid API Key"): `mysql-connector-j` e
      `opentelemetry-semconv`/`-incubating`, ambas herdadas do BOM do
      Quarkus. Corrigidas via override de versão em `dependencyManagement`
      (ver item de segurança abaixo) — CI 100% verde depois disso.
- [x] **CVE real corrigida**: `mysql-connector-j:9.7.0` (5 CVEs, CVSS até
      8.5) e `opentelemetry-semconv`/`opentelemetry-semconv-incubating:1.41.1/1.40.0-alpha`
      (CVSS 7.3+), achadas pelo primeiro scan completo do Dependency-Check
      nos dois serviços. Corrigido sobrescrevendo a versão no
      `dependencyManagement` de cada `pom.xml` (`mysql-connector-j` →
      `26.7.0`, `opentelemetry-semconv`/`-incubating` → `1.43.0`/`1.43.0-alpha`),
      declarado **antes** do import do `quarkus-bom` (Maven resolve por
      "primeira ocorrência vence"). Validado: suíte completa dos dois
      serviços (MySQL real via Dev Services) passou sem alteração de
      código — 59 + 45 testes.

## Próxima fatia (preview — não detalhar ainda)

Fatia 2 do roadmap: `card-service`. Escrever o contrato OpenAPI em
`docs/specs/card-service.yaml` **antes** de detalhar tasks aqui (spec-driven,
ver `CLAUDE.md`).
