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
tem **registrar**, **listar** e **cancelar transação** funcionando ponta a
ponta contra o `account-service` real, inclusive via `docker compose up`
com autenticação de verdade (issuer do Keycloak corrigido) — 2026-08-07.
Falta CI e o resto dos casos de uso de `transaction-service`
(editar/resumo/recorrentes).

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
- [ ] Caso de uso: editar transação (`PUT /transacoes/{id}`) — se valor mudou
      e a transação já estava `CONFIRMADA`, reverte o efeito antigo no saldo
      e aplica o novo, síncrono com `account-service`. Testar especificamente
      o caso de falha do `account-service` no meio do ajuste (não pode deixar
      saldo inconsistente).
- [ ] Caso de uso: resumo por categoria (`GET /transacoes/resumo-por-categoria`)
      — agregação usada pelo dashboard (fatia 6) e pela tool de IA (fatia 5);
      implementar uma vez, os dois consumidores chamam o mesmo endpoint.
- [ ] Testes de integração com Testcontainers cobrindo o `account-service`
      real (Docker), não só o mock via `QuarkusMock` — útil pra pegar
      divergência de contrato entre os dois serviços que o mock não pegaria.

### Transações recorrentes (`transaction-service`)

Conforme `docs/specs/transaction-service.yaml` (endpoints
`/api/v1/transacoes-recorrentes*`) e ADR-0009 — pré-requisito pra fatia 5
(`ai-service` chamar `criar_transacao` com recorrência, PRD 3.5).

- [ ] Entidade `TransacaoRecorrente` (domínio): frequência (só `MENSAL` no
      v1), `quantidadeOcorrencias` nullable (null = indefinida),
      `ocorrenciasGeradas`, status (`ATIVA|PAUSADA|CANCELADA|CONCLUIDA`).
- [ ] Caso de uso: criar regra recorrente → gera a 1ª ocorrência (`Transacao`
      com `transacaoRecorrenteId` preenchido) imediatamente, síncrono com
      `account-service` igual a uma transação normal.
- [ ] Job agendado (Quarkus Scheduler) que roda periodicamente e gera a
      próxima ocorrência de cada regra `ATIVA`, respeitando
      `quantidadeOcorrencias` — ao atingir o limite, regra vira `CONCLUIDA`
      automaticamente. Testar com tempo controlado (não depender de
      `Thread.sleep`/tempo real no teste).
- [ ] Caso de uso: cancelar regra (exclusão lógica, `CANCELADA`) — não afeta
      ocorrências já geradas.
- [ ] Endpoints REST (`POST`/`GET`/`GET {id}`/`DELETE`) conforme o contrato.
- [ ] Testes unitários: geração de ocorrência respeitando limite, regra
      indefinida nunca conclui sozinha, cancelamento não gera mais
      ocorrências.
- [ ] Testes de integração (Testcontainers) cobrindo o job agendado de
      ponta a ponta.

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
- [ ] `.github/workflows/ci.yml` (ADR-0018): build + testes + scan de
      vulnerabilidade (ADR-0017) em todo PR, com path filter (só builda o
      serviço que mudou). CD (deploy) fica pra quando chegarmos na fatia 9 —
      não faz sentido configurar deploy antes de ter servidor de produção
      preparado, mas o CI (sem o D) já deve rodar desde essa fatia.

## Próxima fatia (preview — não detalhar ainda)

Fatia 2 do roadmap: `card-service`. Escrever o contrato OpenAPI em
`docs/specs/card-service.yaml` **antes** de detalhar tasks aqui (spec-driven,
ver `CLAUDE.md`).
