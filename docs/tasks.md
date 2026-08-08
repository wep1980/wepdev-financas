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
`transaction-service` → `account-service`).

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

- [ ] Scaffold do projeto Quarkus (`services/card-service`), porta 8083,
      banco `card_db` — mesma estrutura de camadas (`domain`/`application`/
      `infrastructure`) e mesmas extensions-base do `transaction-service`
      (`rest-client-jackson`, `rest-client-oidc-filter`, `smallrye-jwt`,
      `hibernate-orm-panache`, `flyway`, `hibernate-validator`).
      `docker-compose.yml` ganha serviço `card-service` + variável
      `CARD_SERVICE_CLIENT_SECRET`; `infra/keycloak/realm-financas.json`
      ganha client/service-account `card-service` (role `service`, pra
      chamar `account-service` — mesmo padrão do `transaction-service`).
- [ ] Domínio `Cartao` + caso de uso **criar cartão** (`POST /cartoes`) —
      valida `contaPagamentoId` contra o `account-service` de forma
      síncrona (reusa o 404 do `GET /contas/{id}` pra confirmar posse,
      mesmo padrão de `AccountServiceClientImpl.confirmarPosse()` do
      `transaction-service` — dá pra copiar o client quase pronto).
- [ ] Casos de uso: **listar cartões**, **buscar por id**, **atualizar**,
      **excluir** (lógico) — mesmo padrão anti-IDOR (404 pra inexistente
      OU de outro usuário) já usado nos outros dois serviços.
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
- [ ] Testes: domínio (`Cartao`, `Fatura`), casos de uso (Mockito, cobrindo
      sucesso + 404 anti-IDOR + edge cases de parcelamento/arredondamento),
      integração REST (`@QuarkusTest`, `@TestSecurity`+`@JwtSecurity`, mock
      do `AccountServiceClient` via `QuarkusMock` — mesmo padrão dos outros
      dois serviços). Nenhuma classe fecha sem teste (CLAUDE.md princípio 3).
- [ ] CI: `.github/workflows/ci.yml` ganha job `card-service` (mesmo
      template dos outros dois — `changes` já precisa incluir
      `services/card-service/**` no filtro do `dorny/paths-filter`).
- [ ] `dependency-check-maven` + JaCoCo + build de imagem Docker de
      validação no `pom.xml` do `card-service` (copiar config dos outros
      dois, já validada).
- [ ] Validação de ponta a ponta via containers reais (curl/Postman):
      criar cartão → lançar compra parcelada → conferir fatura(s) geradas
      → pagar fatura fechada → conferir débito na conta de pagamento.
- [ ] README do serviço (`services/card-service/README.md`).
- [ ] Atualizar `docs/postman/mudancas-manuais.txt` (nova collection ou
      import inicial — primeira vez desse serviço, ver seção 2 do
      `docs/postman/README.md`) e `docs/architecture/diagrams.md` (diagrama
      de classe do domínio `Cartao`/`Fatura`, seção nova) conforme as
      classes forem criadas — não deixar acumular pro final.

## Próxima fatia (preview — não detalhar ainda)

Fatia 3 do roadmap: `document-service` (parsing de fatura/extrato/boleto).
Escrever o contrato OpenAPI em `docs/specs/document-service.yaml` **antes**
de detalhar tasks aqui (spec-driven, ver `CLAUDE.md`).
