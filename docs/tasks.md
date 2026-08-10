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
confirmação (PRD 3.2). Primeira fatia vertical cobre **só fatura de cartão
em PDF** — extrato, boleto e foto ficam pra fatias seguintes.

Decisões já tomadas (todas confirmadas pelo usuário em 2026-08-09):
- ADR-0015 aceita: ingestão por foto usa visão do LLM direto, sem OCR
  separado (só relevante quando a fatia de foto chegar).
- Provedor de LLM pra dev/teste: **Ollama local**, já subido em
  `docker-compose.yml` (serviço `ollama`, modelo `llama3.1` já baixado —
  ver `docs/architecture/adr/0002-abstracao-provedor-llm.md`). Modelo de
  visão (`llava`/`bakllava`) só entra quando a fatia de foto começar.
- ADR-0023 (novo): primeira fatia **não integra com `card-service`** —
  lançamento confirmado vira `Transacao` avulsa comum, seguindo o fluxo já
  desenhado em `docs/architecture/overview.md` seção 3. Biblioteca de
  extração de texto de PDF: **Apache PDFBox**.
- Spec escrita: `docs/specs/document-service.yaml` (upload multipart,
  listar, buscar detalhe, confirmar lançamentos).

Backlog de implementação (mirror do nível de detalhe da fatia 2):
1. ✅ Scaffold `services/document-service/` (copiado de `card-service`:
   `pom.xml`, `mvnw`, Dockerfiles, `application.properties` porta 8084,
   `.gitignore`/`.dockerignore`, README). Sem cliente OIDC de saída — nenhum
   endpoint interno síncrono precisa dele nessa fatia (só publica Kafka).
   `pom.xml` já com `quarkus-mongodb-panache` e Apache PDFBox (ADR-0023),
   compilação validada (`./mvnw compile`). Ainda sem domínio/persistência/
   REST — próximo passo é o item 2. `docker-compose.yml`/CI/Keycloak/banco
   `document_db` ficam pro item 11, quando o serviço já tiver algo rodável.
2. ✅ Domínio (`DocumentoImportado` aggregate root, `LancamentoPendente`
   filha, enums `TipoDocumento`/`StatusDocumento`/`StatusLancamento`/
   `TipoLancamento`, exceções) + persistência: `DocumentoImportadoEntity`
   no MongoDB (metadados + bytes do PDF via `quarkus-mongodb-panache`) e
   `LancamentoPendenteEntity` no MySQL (`V1__create_lancamentos_pendentes_table.sql`,
   queryable, `documento_id` referencia o id do documento no Mongo, sem FK
   real). `DocumentoRepositoryImpl` orquestra os dois bancos (sem
   transação distribuída — grava documento antes dos lançamentos, pior
   caso é documento sem lançamento, nunca lançamento órfão). 15 testes de
   domínio (`DocumentoImportadoTest`, `LancamentoPendenteTest`) cobrindo o
   ciclo RECEBIDO→PROCESSANDO→AGUARDANDO_CONFIRMACAO→CONFIRMADO/
   ERRO_PROCESSAMENTO e a idempotência de `confirmar()`. Persistência ainda
   sem teste de integração próprio — será exercida pelos testes REST do
   item 7 (mesmo padrão do card-service).
3. ✅ Porta `LlmProvider` (ADR-0002, só `chat()`/`isConfigured()` — `embed()`
   e suporte a imagem ficam pra quando RAG/foto forem implementados) +
   adapter `OllamaLlmProvider`, via `OllamaRestClient` (`@RegisterRestClient`,
   `POST /api/generate`, timeout de 120s — inferência local em CPU é lenta).
   Contrato JSON (`model`/`prompt`/`stream` → `response`) validado com
   `curl` real contra o container `financas-ollama` já rodando. Sem teste
   automatizado dedicado pro adapter (mesmo padrão do `AccountServiceClientImpl`
   do card-service — thin adapter, `testing-strategy.md` seção 4 manda mockar
   a porta `LlmProvider` nos testes de quem a usa, não testar contra o
   Ollama real em CI).
4. ✅ Extração de texto do PDF via Apache PDFBox (`ExtratorTexto` port +
   `PdfBoxExtratorTexto` — `PdfIlegivelException` se não tiver texto) +
   `AgenteExtracaoFaturaService` (aplicação): monta prompt, chama
   `LlmProvider.chat()` pedindo JSON, faz parsing best-effort da resposta
   (item individual mal-formado é descartado, não derruba a extração
   inteira; resposta que não é JSON válido vira lista vazia). Prompt e
   contrato JSON foram iterados com chamadas reais ao Ollama
   (`llama3.1`, 2026-08-09) até funcionar de verdade, não só no papel —
   achados que mudaram o design:
   - Pedir **array JSON no nível raiz** faz o modelo, às vezes, devolver um
     objeto solto em vez de lista de um item quando só há um lançamento.
     Fix: envelopar num objeto nomeado (`{"anoReferencia", "lancamentos"}`).
   - Pedir pro LLM **montar a data ISO ele mesmo** (a partir de "05/08" no
     texto) errou dia/mês. Fix: pedir `dataTexto` exatamente como escrita
     no documento + um `anoReferencia` separado (extração simples, mais
     confiável), e completar a data em código Java determinístico
     (`LancamentoExtraidoDto.parsearData`), nunca pedir aritmética de data
     ao LLM.
   - LLM às vezes devolve valor negativo pro estorno mesmo instruído a
     sempre usar positivo — `normalizarValor` aplica `.abs()` (sinal já é
     expresso pelo campo `tipo`, mesma convenção do transaction-service).
   - `temperature=0.1` na chamada Ollama (antes era o default, mais
     variável) — mais estável pra extração estruturada.
   22 testes (domínio + `AgenteExtracaoFaturaServiceTest`, mockando
   `ExtratorTexto`/`LlmProvider` per `testing-strategy.md` seção 4, +
   `PdfBoxExtratorTextoTest` com PDF real gerado em memória).
5. ✅ Validação de ponta a ponta com fatura real (2026-08-09) — usuário
   colocou uma fatura Santander de verdade em `test-data/` (pasta nova,
   gitignored — nunca versiona dado financeiro real de terceiro, ver
   `test-data/README.md`). Isso forçou três achados que mudaram o código
   antes mesmo do endpoint existir:
   - **PDF protegido por senha** é comum em fatura de banco brasileira (CPF
     do titular). `ExtratorTexto`/`PdfBoxExtratorTexto` ganharam parâmetro
     `senha` (nulo = sem proteção) + `PdfProtegidoPorSenhaException`.
   - **Conflito de porta real**: existe um Ollama nativo instalado no
     Windows (fora do Docker) escutando `127.0.0.1:11434` especificamente,
     "roubando" conexão IPv4 de `localhost:11434` mesmo com o container
     publicando a mesma porta (bind mais específico vence o wildcard do
     Docker Desktop) — resultava em 404 confuso ("model not found") vindo
     do Ollama errado. Fix: porta do container remapeada pra `11500` no
     host (`docker-compose.yml`), `ollama:11434` continua igual dentro da
     rede Docker.
   - **Fatura com titular + dependentes**: cada pessoa tem seção própria no
     texto (ex: `"JOAO P SANTOS -  4000 XXXX XXXX 0002"`, nome
     abreviado — usuário digitaria "João Paulo Santos" por extenso).
     Pedir pro LLM filtrar a seção certa num prompt de 14KB não funcionou
     de forma confiável (o modelo às vezes inventava outro esquema JSON).
     Fix: `AgenteExtracaoFaturaService` agora recorta a seção certa em
     código Java determinístico ANTES de montar o prompt (por regex no
     cabeçalho de cada cartão, exigindo primeiro E último "nome" do filtro
     baterem — evita falso positivo com sobrenome compartilhado entre
     titular/dependente), e também detecta o ano da fatura pela linha
     "Vencimento" — o LLM só recebe texto já filtrado (~5KB em vez de
     14KB) e só precisa extrair lançamentos, não mais filtrar nem inferir
     ano. Resultado real: 38 lançamentos extraídos, corretamente restritos
     à seção do dependente, valores/datas conferidos manualmente contra o
     PDF. Duas imperfeições conhecidas (aceitáveis pro fluxo "usuário
     confirma", PRD 3.2): 2 de 10 parcelas idênticas de um mesmo
     comerciante foram descartadas pelo LLM, e 3 estornos vieram como
     `DESPESA` em vez de `RECEITA`.
   - **Risco pra fatia 6 anotado, não resolvido ainda**: inferência local
     em CPU pra uma seção de ~5KB levou vários minutos (não segundos) pra
     gerar ~40 lançamentos em JSON. O endpoint de upload da spec é síncrono
     ("extrai de forma síncrona" — resposta 201 já com os lançamentos). Pra
     fatura grande isso pode não ser viável como uma única requisição HTTP
     síncrona — revisitar quando o item 6 for implementado (o próprio
     domínio já modela um estado `PROCESSANDO` intermediário, então dá pra
     virar assíncrono sem redesenhar o modelo, só a spec/REST).
   62 testes no total agora (2 novos no `PdfBoxExtratorTextoTest` pra PDF
   protegido, 2 novos no `AgenteExtracaoFaturaServiceTest` pro recorte
   determinístico) + 1 teste manual/exploratório (`ExtracaoFaturaRealManualTest`,
   não roda em CI — se auto-pula se `test-data/` estiver vazia ou sem
   `FATURA_TESTE_SENHA`, único jeito de rodar de novo essa validação real).
6. ✅ `POST /api/v1/documentos` — upload **assíncrono** (ADR-0024, decisão
   tomada em cima do risco anotado no item 5: extração real leva minutos,
   não dá pra segurar a requisição HTTP). Resposta 202 imediata com status
   `RECEBIDO`; `UploadDocumentoUseCase` persiste (transação síncrona,
   `DocumentoRepositoryImpl.salvar()` agora `@Transactional`) e só DEPOIS
   despacha `ProcessarDocumentoService` via `ManagedExecutor.runAsync()` —
   nessa ordem, nunca a inversa, pra garantir que o documento já está
   commitado antes do job em background tentar buscá-lo numa outra thread.
   `ProcessarDocumentoService` faz `RECEBIDO→PROCESSANDO` (salva),
   chama `AgenteExtracaoFaturaService.extrair()`, e conclui
   `AGUARDANDO_CONFIRMACAO` (achou algo) ou `ERRO_PROCESSAMENTO` (nada
   encontrado, PDF ilegível/protegido, ou erro inesperado — sempre com
   `mensagemErro` claro, nunca dado sensível logado). Achado técnico: leitura
   JPA/Hibernate ORM Panache também precisa de transação/contexto ativo
   quando chamada de uma thread do `ManagedExecutor` (fora do request HTTP)
   — `buscarPorId()` também ganhou `@Transactional`, senão
   `ContextNotActiveException`.
7. ✅ `GET /api/v1/documentos`, `GET /api/v1/documentos/{id}` — listagem e
   detalhe, escopados ao usuário autenticado (mesmo padrão de isolamento por
   `sub` já usado nos outros serviços). Cliente usa o GET por id pra fazer
   polling do status até sair de `PROCESSANDO`.

   Testes dos itens 6+7: 24 novos (5 casos de uso unitários +
   `DocumentoResourceTest` com 9 cenários de integração, `@QuarkusTest` +
   Testcontainers Mongo/MySQL, `LlmProvider` mockado via `QuarkusMock`
   igual ao `AccountServiceClientImpl` do card-service, `Awaitility` pra
   esperar o processamento assíncrono terminar sem sleep arbitrário — nova
   dependência de teste, único serviço com fluxo assíncrono até agora).
   Achado de configuração: MongoDB com `@BsonId` do tipo `UUID` precisa de
   `quarkus.mongodb.uuid-representation=standard` explícito, senão
   `CodecConfigurationException` ao gravar. 70 testes no total agora.

   Validado também com uma segunda fatura real (Itaú, sem senha, pessoa
   única — sem seção titular/dependente): 10 lançamentos extraídos, soma
   batendo 100% com o "Total dos lançamentos atuais" do PDF (R$ 1.500,00).
   Resultado bem mais limpo que a fatura Santander (nenhum item perdido,
   nenhum tipo errado) — o modelo até excluiu sozinho, sem instrução
   explícita no prompt, o "Pagamento efetuado" (não é despesa, é
   pagamento da fatura anterior) e a parcela futura de "próxima fatura".
   Único ruído: campo `categoriaSugerida` de 2 lançamentos PIX veio com o
   texto de detalhamento de juros em vez de uma categoria — cosmético, sem
   efeito no fluxo (é só sugestão, best-effort).
8. ✅ `POST /api/v1/documentos/{id}/confirmar` — marca lançamentos selecionados
   CONFIRMADO/os demais REJEITADO, publica UM evento Kafka
   `documento.lancamentos-confirmados` (com a lista de lançamentos
   confirmados + `contaId`), idempotente se já CONFIRMADO (não republica).
   Gap real encontrado ao desenhar isso: a spec original não tinha como o
   `transaction-service` saber EM QUAL conta debitar/creditar — o
   document-service não sabe disso sozinho (ADR-0023, sem integração com
   card-service). Corrigido adicionando `contaId` obrigatório em
   `ConfirmarLancamentosRequest` — o usuário escolhe a conta no momento da
   confirmação, não no upload. `422` novo pra "nenhum lançamento
   selecionado" (`NenhumLancamentoSelecionadoException`) e pra "documento
   ainda não processado" (`DocumentoAindaNaoProcessadoException`, já
   existia no domínio desde o item 2, só faltava o mapper REST). 19 testes
   novos (5 unitários em `ConfirmarLancamentosUseCaseTest` + 5 cenários de
   integração no `DocumentoResourceTest`). Corrigido também um bug real no
   `ExtracaoFaturaRealManualTest` (item 5): sem `FATURA_TESTE_ARQUIVO`
   explícito, ele pegava o primeiro PDF em ordem alfabética e quebrava
   `mvn test` se esse PDF exigisse senha — agora exige escolha explícita
   do arquivo pra rodar, senão pula (self-skip de verdade). 58 testes no
   total do `document-service` agora, suite inteira verde.

   Validado também com a fatura Nubank (PDF fechado): 11 lançamentos, soma
   batendo com o subtotal do cartão na fatura. Achado real no caminho: o
   Nubank escreve data como "10 JUN" (dia + mês abreviado em português) e
   vencimento como "17 JUL 2026" — formato bem diferente do "DD/MM" do
   Santander/Itaú. `LancamentoExtraidoDto.parsearData` e
   `AgenteExtracaoFaturaService.detectarAnoReferencia` ganharam um segundo
   formato de parsing (mapeamento manual JAN–DEZ, não locale-based —
   mais confiável que depender de `Locale("pt","BR")` do Java, que usa
   abreviação com ponto minúsculo, não bate com o formato do banco). Sem
   esse fix, os 11 lançamentos vinham certos do LLM mas todos eram
   descartados na hora de converter a data (best-effort silencioso —
   só percebido inspecionando o log).
9. ✅ Consumer Kafka em `transaction-service` pro tópico
   `documento.lancamentos-confirmados` — cria `Transacao` (DESPESA/RECEITA)
   por lançamento confirmado. Achado real ao desenhar isso, registrado em
   [ADR-0025](architecture/adr/0025-confirmacao-posse-conta-antes-do-evento.md):
   o padrão existente de débito (`RegistrarTransacaoUseCase` →
   `AccountServiceClient.debitar()`) confirma posse da conta propagando o
   **token do usuário do request HTTP** — um consumer Kafka não tem
   requisição em andamento, não tem token pra propagar. Resolvido
   confirmando posse **uma vez só**, no `document-service`, dentro de
   `ConfirmarLancamentosUseCase` (item 8, síncrono, token disponível) —
   o consumer nunca reverifica, vai direto pro débito/crédito via cliente
   de serviço (`debitarSemConfirmarPosse`/`creditarSemConfirmarPosse`,
   métodos novos na porta `AccountServiceClient`). `document-service`
   ganhou sua própria integração com `account-service` (não tinha
   nenhuma até agora — não é integração com card-service, isso continua
   fora de escopo, ADR-0023).

   Validado com um teste de integração publicando um evento JSON real no
   tópico via Kafka Dev Services (mesmo broker que o consumer ouve) —
   fiação de ponta a ponta confirmada (deserializer, consumer, caso de
   uso, débito real mockado no account-service). Achado técnico: leitura
   de repositório dentro do corpo de um teste (fora de request HTTP)
   também precisa de `QuarkusTransaction.requiringNew().call(...)` —
   mesma família do `ContextNotActiveException` já visto no
   document-service com `ManagedExecutor`.

   14 testes novos (4 unitários em `ProcessarLancamentosConfirmadosUseCaseTest`,
   1 unitário no mapeamento do consumer, 1 de integração Kafka real, +
   testes do lado document-service pro `AccountServiceClient` novo). 97
   testes no total do `transaction-service`, suite inteira verde.

   **Fluxo de ponta a ponta completo agora**: upload de fatura → extração
   via LLM → confirmação (com verificação de posse de conta) → evento
   Kafka → consumer → `Transacao` criada com débito real no
   `account-service` — exatamente o diagrama de sequência de
   `overview.md` seção 3, de ponta a ponta, validado com testes de
   integração reais (não só unitários).
10. ✅ Testes em todas as camadas — cobertura feita junto de cada item (6–9),
    isolamento por `sub`/mock dedicado por teste desde o início (lição já
    virada hábito). 100+ testes no `document-service`, 97 no
    `transaction-service` (+14 do consumer novo). Nenhum caso de borda
    óbvio faltando — não abrindo um item de "testes gerais" à parte.
11. ✅ `docker-compose.yml`: serviço `document-service` novo (depende de
    mysql/mongo/kafka/ollama/account-service), validado subindo de
    verdade contra a stack real (build + start limpo, healthcheck 200).
    `document_db` criado (init script + aplicado no container já
    rodando). CI: job `document-service` novo em `ci.yml` (mesmo template
    dos outros 3 — testes, cobertura JaCoCo, build Docker, dependency-check),
    `document-service` no filtro de paths do job `changes`. Keycloak:
    client `document-service` registrado — **sem** service-account (ao
    contrário dos outros serviços): `document-service` só chama
    `account-service` propagando o token do usuário (ADR-0025), nunca via
    client_credentials, então `bearerOnly: true` é o suficiente (aplicado
    também no container Keycloak já rodando). Postman: 4 endpoints novos
    documentados em `mudancas-manuais.txt` + variável
    `document_service_url` no environment. `diagrams.md`: container graph
    atualizado (não mais "planejado", nova aresta síncrona pro
    account-service), seção 3.4 nova (ER conceitual de
    `DocumentoImportado`/`LancamentoPendente`). `overview.md`: tabela de
    serviços + diagrama de sequência da seção 3 reescritos pra refletir o
    fluxo assíncrono de verdade (upload 202 + polling + confirmação com
    ADR-0025), não mais o desenho original síncrono. `roadmap.md`: fatia 3
    marcada `✅ Entregue`. `README.md` raiz: parágrafo do `document-service`
    no "Estado atual", tabela de endpoints, URLs (Mongo, Ollama).

**Fatia 3 (`document-service`, fatura PDF) entregue.** Extrato bancário
(PDF/CSV), boleto de financiamento e ingestão por foto (mobile) ficam pra
uma fatia futura — não redetalhar aqui até decidir retomar.

## Próxima fatia (preview — não detalhar ainda)

Fatia 4 do roadmap: `budget-service` — orçamento por categoria/mês,
cálculo de "disponível pra gastar" (PRD 3.3).
