# Tasks — Fatia Atual

> Backlog de trabalho em detalhe, só da fatia em andamento (roadmap #3). Ao
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
cobertura na PR). Detalhe completo: `docs/historico.md` (2026-08-06 a
2026-08-08) e o histórico de commits do git — não repetido aqui.

Bugs reais encontrados e corrigidos nessa fatia (pra não esquecer a causa
se algo parecido aparecer de novo): issuer do Keycloak variando por Host
da requisição dentro de container Docker; `mvnw` sem bit de execução no
Windows (`core.filemode=false`); `GITHUB_TOKEN` com permissão mínima em PR
de fonte externa (Dependabot); duas CVEs reais (`mysql-connector-j`,
`opentelemetry-semconv`) herdadas do BOM do Quarkus, corrigidas com
override de versão.

## Fatia 2 — `card-service` — ✅ Concluída (2026-08-08/09)

Spec: `docs/specs/card-service.yaml`. Decisão de arquitetura em
[ADR-0022](architecture/adr/0022-card-service-independente-de-conta.md):
`card-service` é independente de `TipoConta.CARTAO_CREDITO` — todo
`Cartao` tem `contaPagamentoId` apontando pra uma `Conta`
(`CORRENTE`/`POUPANCA`/`CARTEIRA`) que paga a fatura, débito síncrono no
pagamento. CRUD de cartão, lançar compra (parcelamento com distribuição
automática em faturas consecutivas, criadas sob demanda, arredondamento
absorvido na última parcela), listar/buscar fatura, pagar fatura
(idempotente, síncrono com `account-service`), job de fechamento
automático (`FecharFaturasVencidasJob`, núcleo testável com data como
parâmetro — mesmo padrão do `GerarOcorrenciasRecorrentesJob`) e endpoint
interno de próximos vencimentos (role `service`). 82 testes, CI verde
(job próprio com JaCoCo + build Docker + dependency-check), validado de
ponta a ponta contra containers reais (incluindo débito real no
`account-service` ao pagar fatura). Detalhe completo:
`docs/historico.md` (2026-08-08/09).

## Fatia 3 — `document-service`

Parsing de fatura de cartão (PDF), extrato bancário (PDF/CSV) e boleto de
financiamento (PDF/foto — ADR-0014/0015), gerando transações pendentes de
confirmação (PRD 3.2). Ainda **sem contrato OpenAPI** — antes de detalhar
tasks aqui, escrever `docs/specs/document-service.yaml` (spec-driven, ver
`CLAUDE.md`) e revisar as decisões já tomadas em ADR-0004 (lista de tipos
de documento extensível), ADR-0014 (linha digitável antes de LLM pro
boleto) e ADR-0015 (foto via mobile, visão LLM).

## Próxima fatia (preview — não detalhar ainda)

Fatia 4 do roadmap: `budget-service` — orçamento por categoria/mês,
cálculo de "disponível pra gastar" (PRD 3.3).
