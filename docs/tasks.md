# Tasks — Fatia Atual

> Backlog de trabalho em detalhe, só da fatia em andamento (roadmap #2). Ao
> concluir a fatia, arquive as tasks completas (ou simplesmente comece um
> bloco novo pra próxima fatia) e não deixe esse arquivo virar um histórico
> gigante — histórico de decisão fica em ADR/git/`docs/historico.md`, não aqui.

## Fatia 1 — Contas + Transações (back-end) — ✅ Concluída (2026-08-07/08)

`account-service`: CRUD completo (criar/listar/buscar/atualizar/excluir) +
débito/crédito, 45 testes. `transaction-service`: registrar/listar/cancelar/
editar/resumo por categoria/transações recorrentes (com job agendado), 91
testes. Os dois rodando ponta a ponta via `docker compose up` com
autenticação de verdade. CI 100% verde (`mvn test` + cobertura JaCoCo +
build de imagem Docker de validação + `dependency-check` + comentário de
cobertura na PR). Detalhe completo de cada item, bug encontrado e decisão
tomada: `docs/historico.md` (entradas de 2026-08-06 a 2026-08-08) e o
histórico de commits do git — não repetido aqui.

Bugs reais encontrados e corrigidos nessa fatia (pra não esquecer a causa
se algo parecido aparecer de novo): issuer do Keycloak variando por Host
da requisição dentro de container Docker; `mvnw` sem bit de execução no
Windows (`core.filemode=false`); `GITHUB_TOKEN` com permissão mínima em PR
de fonte externa (Dependabot); duas CVEs reais (`mysql-connector-j`,
`opentelemetry-semconv`) herdadas do BOM do Quarkus, corrigidas com
override de versão.

## Fatia 2 — `card-service`

Spec pronta: `docs/specs/card-service.yaml`. Decisão de arquitetura
registrada em [ADR-0022](architecture/adr/0022-card-service-independente-de-conta.md):
`card-service` é **independente** de `TipoConta.CARTAO_CREDITO` do
`account-service` — todo `Cartao` tem um `contaPagamentoId` apontando pra
uma `Conta` (`CORRENTE`/`POUPANCA`/`CARTEIRA`) que paga a fatura, débito
síncrono no momento do pagamento (mesmo padrão já validado em
`transaction-service` → `account-service`). **CRUD de cartão entregue**
(criar/listar/buscar/atualizar/excluir, 34 testes, CI verde, porta 8083)
— 2026-08-08/09. Falta fatura e parcelamento.

Domínio (conforme a spec):
- **`Cartao`** — apelido, bandeira (opcional), limite, `diaFechamento`,
  `diaVencimento`, `contaPagamentoId`, `ativo` (exclusão lógica).
- **`Fatura`** — por cartão + competência (AAAA-MM), `dataFechamento`,
  `dataVencimento`, `valorTotal` (soma das parcelas), status
  `ABERTA|FECHADA|PAGA`. Criada automaticamente (não tem endpoint de
  criação manual) quando a 1ª compra de uma competência é lançada.
- **Compra/Parcela** — uma compra (`quantidadeParcelas`, `valorTotal`)
  gera 1..N parcelas, cada uma numa fatura diferente (competências
  consecutivas a partir da 1ª fatura afetada, decidida por
  `dataCompra` vs `diaFechamento` do mês corrente).

Ordem sugerida (vertical, mesmo padrão de `transaction-service` —
implementar/testar/validar/documentar um item de cada vez, não tudo junto):

- [x] Scaffold do projeto Quarkus (`services/card-service`), porta 8083,
      banco `card_db` — mesma estrutura de camadas (`domain`/`application`/
      `infrastructure`) e mesmas extensions-base do `transaction-service`
      (`rest-client-jackson`, `rest-client-oidc-filter`, `smallrye-jwt`,
      `hibernate-orm-panache`, `flyway`, `hibernate-validator`, `scheduler`
      — já incluído mesmo sem uso ainda, vai precisar pro job de fechar
      fatura). `docker-compose.yml` ganhou serviço `card-service` + variável
      `CARD_SERVICE_CLIENT_SECRET`; `infra/keycloak/realm-financas.json`
      ganhou client/service-account `card-service` (role `service`) —
      aplicado tanto no JSON (pra deploy futuro) quanto no Keycloak já
      rodando via `kcadm.sh` (o import só roda na 1ª subida do container,
      igual já tinha acontecido com `transaction-service`). `infra/mysql/init/01-databases.sql`
      ganhou `card_db` (criado também manualmente no container já rodando,
      mesmo motivo).
- [x] Domínio `Cartao` + caso de uso **criar cartão** (`CriarCartaoUseCase`,
      `POST /cartoes`) — confirma `contaPagamentoId` contra o
      `account-service` de forma síncrona (`AccountServiceClient.confirmarPosseDaConta()`,
      reusa o 404 do `GET /contas/{id}` — mesmo client HTTP copiado quase
      pronto do `transaction-service`, incluindo `PropagarAutorizacaoHeadersFactory`)
      **antes** de persistir — sem cartão "órfão" de conta inválida.
- [x] Casos de uso: **listar cartões ativos**, **buscar por id**,
      **atualizar** (reconfirma posse de `contaPagamentoId`, que pode ter
      mudado), **excluir** (lógico, idempotente) — mesmo padrão anti-IDOR
      (404 pra inexistente OU de outro usuário) já usado nos outros dois
      serviços. 7 testes de domínio (`CartaoTest`) + testes de caso de uso
      pra cada operação + 12 de integração REST (`CartaoResourceTest`,
      mock do `AccountServiceClient` via `QuarkusMock`) — 34 testes no
      total do serviço, todos passando. Validado de ponta a ponta contra
      containers reais: criar conta de pagamento → criar cartão → listar →
      buscar → atualizar (troca `contaPagamentoId`) → excluir (idempotente,
      cartão inativo some da listagem mas continua buscável por id) →
      criar cartão com `contaPagamentoId` inexistente dá 404. CI ganhou o
      job `card-service` (mesmo template dos outros dois — testes +
      cobertura JaCoCo + build de imagem Docker de validação +
      dependency-check).
- [ ] Domínio `Fatura` + `ParcelaCompra` (nome de classe a definir na
      implementação) + caso de uso **lançar compra** (`POST
      /cartoes/{id}/compras`) — decide a fatura de destino da 1ª parcela
      (`dataCompra` antes do `diaFechamento` do mês corrente → fatura do
      mês corrente; senão → mês seguinte), cria a fatura se não existir
      ainda, distribui as parcelas seguintes em faturas subsequentes
      (criando cada uma). Arredondamento de `valorTotal/quantidadeParcelas`
      — decidir estratégia (ex: sobra fica na última parcela) durante a
      implementação, com teste cobrindo valor não divisível exatamente
      (ex: 100.00 em 3x).
- [ ] Casos de uso: **listar faturas de um cartão** (filtro por status),
      **buscar fatura por id** (com as parcelas).
- [ ] Caso de uso **pagar fatura** (`POST /faturas/{id}/pagar`) — só
      permite fatura `FECHADA` (422 se `ABERTA`), débito síncrono na
      `contaPagamentoId` do cartão (reusar o client HTTP do
      `account-service`, mesmo padrão de dois tokens do
      `transaction-service`), idempotente (pagar fatura já `PAGA` é
      204 sem debitar de novo).
- [ ] Job agendado que **fecha fatura automaticamente** no `diaFechamento`
      (`ABERTA` → `FECHADA`, calcula `valorTotal` definitivo) — mesmo
      padrão do `GerarOcorrenciasRecorrentesJob`: núcleo testável recebendo
      a data como parâmetro, wrapper fino com `@Scheduled` só ligando ao
      relógio real, desabilitado em teste.
- [ ] Endpoint interno `GET /faturas/proximos-vencimentos` (role
      `service`) — mesmo padrão do endpoint análogo em
      `transaction-service`, usado pelo futuro `notification-service`
      (ADR-0010).
- [x] Testes do CRUD de cartão: domínio (`CartaoTest`, 7 casos), casos de
      uso (Mockito, sucesso + 404 anti-IDOR pra cada operação), integração
      REST (`@QuarkusTest`, `@TestSecurity`+`@JwtSecurity`, mock do
      `AccountServiceClient` via `QuarkusMock`) — 34 testes no total,
      todos passando. `Fatura`/parcelamento ainda sem teste (não
      implementados ainda — CLAUDE.md princípio 3 continua valendo quando
      chegar a vez).
- [x] CI: `.github/workflows/ci.yml` ganhou job `card-service` (mesmo
      template dos outros dois, `changes` já inclui
      `services/card-service/**` no filtro do `dorny/paths-filter`) —
      testes + cobertura JaCoCo + build de imagem Docker de validação +
      `dependency-check-maven` (13.0.0, mesma versão), tudo já configurado
      no `pom.xml` do `card-service` (copiado dos outros dois, já
      validado).
- [x] README do serviço (`services/card-service/README.md`).
- [x] `docs/architecture/diagrams.md` — seção 4.4 nova (diagrama de classe
      de `Cartao`, código já existe) — `Fatura`/`Parcela` seguem só no
      modelo conceitual (seção 3.3) até serem implementados.
- [ ] Domínio `Fatura` + `ParcelaCompra` (nome de classe a definir na
      implementação) + caso de uso **lançar compra** (`POST
      /cartoes/{id}/compras`) — decide a fatura de destino da 1ª parcela
      (`dataCompra` antes do `diaFechamento` do mês corrente → fatura do
      mês corrente; senão → mês seguinte), cria a fatura se não existir
      ainda, distribui as parcelas seguintes em faturas subsequentes
      (criando cada uma). Arredondamento de `valorTotal/quantidadeParcelas`
      — decidir estratégia (ex: sobra fica na última parcela) durante a
      implementação, com teste cobrindo valor não divisível exatamente
      (ex: 100.00 em 3x).
- [ ] Casos de uso: **listar faturas de um cartão** (filtro por status),
      **buscar fatura por id** (com as parcelas).
- [ ] Caso de uso **pagar fatura** (`POST /faturas/{id}/pagar`) — só
      permite fatura `FECHADA` (422 se `ABERTA`), débito síncrono na
      `contaPagamentoId` do cartão (reusar o client HTTP do
      `account-service`, mesmo padrão de dois tokens do
      `transaction-service`), idempotente (pagar fatura já `PAGA` é
      204 sem debitar de novo).
- [ ] Job agendado que **fecha fatura automaticamente** no `diaFechamento`
      (`ABERTA` → `FECHADA`, calcula `valorTotal` definitivo) — mesmo
      padrão do `GerarOcorrenciasRecorrentesJob`: núcleo testável recebendo
      a data como parâmetro, wrapper fino com `@Scheduled` só ligando ao
      relógio real, desabilitado em teste.
- [ ] Endpoint interno `GET /faturas/proximos-vencimentos` (role
      `service`) — mesmo padrão do endpoint análogo em
      `transaction-service`, usado pelo futuro `notification-service`
      (ADR-0010).
- [ ] Testes de `Fatura`/parcelamento: domínio, casos de uso (Mockito,
      cobrindo sucesso + 404 anti-IDOR + edge cases de parcelamento/
      arredondamento), integração REST — mesmo padrão do CRUD de cartão.
- [ ] Validação de ponta a ponta via containers reais (curl/Postman):
      lançar compra parcelada → conferir fatura(s) geradas → pagar fatura
      fechada → conferir débito na conta de pagamento.
- [ ] Atualizar `docs/postman/mudancas-manuais.txt` com os endpoints de
      fatura/compra quando implementados, mesmo padrão já usado pro CRUD
      de cartão (ver entrada de 2026-08-08 nesse arquivo).

## Próxima fatia (preview — não detalhar ainda)

Fatia 3 do roadmap: `document-service` (parsing de fatura/extrato/boleto).
Escrever o contrato OpenAPI em `docs/specs/document-service.yaml` **antes**
de detalhar tasks aqui (spec-driven, ver `CLAUDE.md`).
