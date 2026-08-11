# Tasks — Fatia Atual

> Backlog de trabalho em detalhe, só da fatia em andamento (ver `roadmap.md`
> pro número/nome da fatia atual). Ao
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
  > **Superado em 2026-08-11 pela ADR-0028** — usuário pediu exatamente o
  > cruzamento com `card-service` previsto acima como possível adiante
  > ("levar em consideração só as compras novas" + "parcela reduzindo mês
  > a mês sem novo upload", já implementado no `card-service` desde a
  > fatia 2). Upload agora exige `cartaoId`; confirmação lança compra
  > nova no cartão (dedup por assinatura) em vez de publicar evento
  > Kafka. Ver `docs/historico.md` 2026-08-11 pro detalhe completo da
  > implementação e validação.
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

## Fatia 4 — `budget-service`

Orçamento por categoria/mês e cálculo de "disponível pra gastar" (PRD
3.3). Cruza dado de três serviços (account-service, card-service,
transaction-service) de forma síncrona, propagando o token do usuário —
mesmo padrão de dois tokens do ADR-0025.

1. ✅ Spec escrita: `docs/specs/budget-service.yaml`
   (`POST/GET /orcamentos`, `PUT/DELETE /orcamentos/{id}`,
   `GET/PUT /reserva`, `GET /disponivel-para-gastar`). Regra exata de
   cálculo do "disponível pra gastar" fechada e documentada em
   [ADR-0026](architecture/adr/0026-regra-calculo-disponivel-para-gastar.md)
   antes de escrever a spec (a spec dependia da regra estar decidida):
   saldo das contas CORRENTE/CARTEIRA (account-service) − faturas FECHADA
   com vencimento no mês (card-service) − despesas recorrentes ATIVA
   vigentes no mês, aproximadas como "regra ativa = 1 compromisso por mês"
   (transaction-service) − reserva (valor único, definido no próprio
   budget-service). Resposta devolve o detalhamento item a item de cada
   parcela, não só o total — requisito direto do PRD seção 6 (IA precisa
   conseguir explicar de onde tirou o número). Orçamento por categoria é
   independente do cálculo de "disponível pra gastar": usa
   `transaction-service` `GET /transacoes/resumo-por-categoria` (endpoint
   já existente) pra saber quanto já foi gasto numa categoria/mês.
2. ✅ Scaffold `services/budget-service/` (copiado de `card-service`:
   `pom.xml`, `mvnw`, Dockerfiles, `application.properties` porta 8085,
   `.gitignore`/`.dockerignore`, README). `pom.xml` sem
   `quarkus-scheduler` (sem job agendado nessa fatia) e sem
   `quarkus-rest-client-oidc-filter`/`quarkus.oidc-client`
   (client_credentials) — diferente do `card-service`, nenhuma das três
   chamadas de saída precisa de role `service` (ADR-0026, sempre
   propagando o token do usuário). Três blocos de `rest-client` já
   configurados em `application.properties` (account-service:8081,
   card-service:8083, transaction-service:8082). Compilação validada
   (`./mvnw compile`). Ainda sem domínio/persistência/REST — próximo
   passo é o item 3.
3. ✅ Domínio (`Orcamento`, `Reserva`) + persistência. `Orcamento`
   aggregate root: `usuarioId`/`categoria`/`mesReferencia` (`YearMonth`,
   mesmo tipo já usado em `Fatura.competencia` do card-service) fixos
   depois de criado, `valorLimite` editável, `status`
   (`ATIVO`/`CANCELADO`, cancelar é idempotente) — `valorConsumido`/
   `valorDisponivel` propositalmente **fora** do domínio, calculados na
   hora pelo caso de uso (item 5), nunca persistidos. `Reserva` não é um
   aggregate tradicional — uma linha por usuário (`usuarioId` é a própria
   PK), upsert sempre, sem histórico; `Reserva.semDefinir(usuarioId)`
   modela o estado default (valor 0, nunca atualizada) pra quando o
   usuário nunca configurou uma. Sem constraint `UNIQUE` no banco pra
   (usuarioId+categoria+mesReferencia) — cancelar e recriar orçamento pra
   mesma categoria/mês é fluxo válido, então a checagem de duplicata
   (`OrcamentoJaExisteException`, 422) é feita em código
   (`OrcamentoRepository.existeAtivo`) pelo caso de uso de criação (item
   5), não pelo banco. Persistência (`OrcamentoEntity`/`ReservaEntity` +
   mappers + `PanacheRepositoryBase`) segue exatamente o padrão do
   `CartaoRepositoryImpl`/`FaturaEntity` do card-service. 12 testes de
   domínio (`OrcamentoTest`, `ReservaTest`) — sem teste de integração de
   banco ainda, mesmo padrão do `document-service` item 2 (será exercida
   pelos testes REST do item 6, não duplicado aqui).
4. ✅ Clientes de saída: `AccountServiceClient`, `CardServiceClient`,
   `TransactionServiceClient` (propagando token do usuário, ADR-0026).
   Diferente do padrão de card-service/document-service (que confirmam
   posse de um id específico via `AccountServiceUsuarioClient`), os
   endpoints chamados aqui (`GET /contas`, `GET /cartoes`,
   `GET /cartoes/{id}/faturas`, `GET /transacoes-recorrentes`,
   `GET /transacoes/resumo-por-categoria`) já filtram pelo `sub` do token
   — sem id de entrada, sem checagem de posse pra fazer, um único
   `PropagarAutorizacaoHeadersFactory` compartilhado pelos três REST
   clients. `CardServiceClientImpl` orquestra duas chamadas (lista
   cartões ativos, depois faturas `FECHADA` de cada um — card-service não
   tem um endpoint "todas as faturas em aberto do usuário" de uma vez
   só). Filtro por mês (`dataVencimento`/`dataInicio` dentro do mês
   consultado) fica pro caso de uso (item 5), não pro cliente — os
   clientes devolvem tudo, sem opinião sobre "qual mês". Sem teste
   dedicado pros `*ClientImpl` (thin adapter — mesmo padrão já
   estabelecido em `AccountServiceClientImpl` do card-service: mockados
   nos testes de quem usa, item 5/6, nunca testados contra o serviço
   real em CI).
5. ✅ Casos de uso: `CriarOrcamentoUseCase` (rejeita duplicata categoria+mês
   via `existeAtivo` ANTES de persistir, `OrcamentoJaExisteException`),
   `AtualizarOrcamentoUseCase`, `ExcluirOrcamentoUseCase` (idempotente),
   `ListarOrcamentosUseCase`, `DefinirReservaUseCase` (upsert),
   `BuscarReservaUseCase` (nunca 404, `Reserva.semDefinir` quando não
   configurada), `CalcularDisponivelParaGastarUseCase` (formula completa
   da ADR-0026). `OrcamentoDetalhe` (application) combina `Orcamento` +
   `valorConsumido` calculado na hora — mesmo padrão de `FaturaDetalhe`
   do card-service. Achado de eficiência: `ListarOrcamentosUseCase` busca
   o resumo por categoria do `transaction-service` **uma vez só** pro mês
   inteiro (não uma chamada por orçamento na lista), montando um mapa
   categoria→gasto reaproveitado. Filtro por mês (fatura por
   `dataVencimento`, despesa recorrente por `dataInicio`) e por tipo de
   conta (`CORRENTE`/`CARTEIRA`) acontece todo dentro de
   `CalcularDisponivelParaGastarUseCase`, não nos clientes (item 4) — só
   ali existe a noção de "qual mês". 30 testes no total (18 novos de
   caso de uso, incluindo verificação explícita de que
   `ListarOrcamentosUseCase` não chama o transaction-service quando não
   há orçamento nenhum), suite inteira verde.
6. ✅ REST + testes de integração. Três resources
   (`OrcamentoResource` — `POST/GET /orcamentos`, `PUT/DELETE /orcamentos/{id}`;
   `ReservaResource` — `GET/PUT /reserva`; `DisponivelParaGastarResource` —
   `GET /disponivel-para-gastar`), DTOs com `static de(...)` (mesmo molde
   de `CartaoResponse`/`FaturaResponse` do card-service; `OrcamentoResponse.de`
   calcula `valorDisponivel`/`percentualConsumido` na borda REST, não no
   caso de uso), mappers de exceção (`OrcamentoNaoEncontradoExceptionMapper`
   → 404, `OrcamentoJaExisteExceptionMapper` → 422,
   `ConstraintViolationExceptionMapper` → 400). Query param `mes`
   (`AAAA-MM`) validado por `@Pattern` direto no método do resource — não
   dá pra usar `YearMonth` como tipo de `@QueryParam` (JAX-RS não
   converte automaticamente, só tipos com `valueOf(String)`/
   `fromString(String)`), então fica `String` validado + `YearMonth.parse`
   manual no corpo do método; já no *corpo* das requisições
   (`CriarOrcamentoRequest.mesReferencia`) `YearMonth` funciona direto —
   Jackson já desserializa via `JavaTimeModule` (mesmo motivo que
   `Instant`/`LocalDate` já funcionavam nos outros serviços). 19 testes de
   integração novos (`OrcamentoResourceTest`, `ReservaResourceTest`,
   `DisponivelParaGastarResourceTest`, `@QuarkusTest` + `QuarkusMock` pros
   três `*ClientImpl`, mesmo padrão do `CartaoResourceTest`). Achado de
   isolamento entre testes (mesma classe, sem rollback automático — lição
   já conhecida desde a fatia 2): dois testes que criavam orçamento
   "Mercado"/2026-08 pro mesmo usuário de outro teste colidiam com
   `OrcamentoJaExisteException` — corrigido com `sub` dedicado por teste
   que precisa de estado isolado. 49 testes no total do `budget-service`,
   suite inteira verde.
7. ✅ `docker-compose.yml`: serviço `budget-service` novo (depende de
   mysql/account-service/card-service/transaction-service), validado
   subindo de verdade contra a stack real (`docker compose up -d --build
   budget-service` — build limpo, Flyway aplicou as duas migrations,
   healthcheck 200). `budget_db` criado (init script + aplicado no
   container já rodando). CI: job `budget-service` novo em `ci.yml`
   (mesmo template dos outros — testes, cobertura JaCoCo, build Docker,
   dependency-check), `budget-service` no filtro de paths do job
   `changes`. Keycloak: client `budget-service` registrado — **sem**
   service-account (mesmo padrão do `document-service`): `budget-service`
   só propaga o token do usuário pros três serviços que chama (ADR-0026),
   nunca via client_credentials (aplicado também no container Keycloak já
   rodando). Postman: 7 endpoints novos documentados em
   `mudancas-manuais.txt` + variável `budget_service_url` no environment.
   `diagrams.md`: container graph sem mais "(planejado)", três arestas
   síncronas novas (account/card/transaction-service), seção 3.5 nova
   (ER conceitual de `Orcamento`/`Reserva`, substituindo a seção
   "Pendente"), seção 4.6 nova (diagrama de classes do domínio).
   `overview.md`: tabela de serviços → ✅ Entregue, seção 6 nova
   (diagrama de sequência do fluxo "disponível pra gastar"), seções
   seguintes renumeradas. `roadmap.md`: fatia 4 → ✅ Entregue, próxima
   ação aponta pra fatia 5 (`ai-service`). `README.md` raiz: parágrafo do
   `budget-service` no "Estado atual", tabela de endpoints, URLs, contagem
   de serviços no CI/docker compose atualizada de quatro pra cinco.

**Fatia 4 (`budget-service`) entregue.** Orçamento por categoria/mês e
"disponível pra gastar" (PRD 3.3), regra de cálculo documentada em
ADR-0026. 49 testes, CI verde, container validado contra a stack real.

## Fatia 5 — `ai-service`

RAG + chat em linguagem natural (PRD 3.4) e execução de ação via comando
de IA, sempre com confirmação explícita (PRD 3.5, ADR-0007). Cruza
account-service/budget-service/card-service/transaction-service via
tools MCP, RAG sobre Qdrant pra busca semântica de transação/lançamento.

Decisão tomada antes de escrever a spec (confirmada pelo usuário,
2026-08-10): **Qdrant confirmado** como vector store — ADR-0005 estava
"proposta, não confirmada" desde 2026-08-06, virou `Aceita` agora, no
início desta fatia.

1. ✅ Spec escrita: `docs/specs/ai-service.yaml`
   (`POST /chat`, `GET /conversas`, `GET /conversas/{id}`,
   `GET/PUT /configuracao`). Decisões de desenho tomadas ao escrever a
   spec:
   - **Um único endpoint pra tudo** (`POST /chat`): pergunta nova,
     comando de ação, correção de proposta pendente e confirmação
     ("sim"/"confirmar") passam todos pelo mesmo `POST /api/v1/chat` — o
     agente orquestrador decide a intenção pelo texto + estado da
     conversa (ai-strategy.md seção 4), não existe um endpoint
     `/confirmar` estruturado separado (diferente do fluxo de documento
     do `document-service`, que usa `POST /confirmar` com ids
     explícitos — aqui a confirmação é conversacional, por design do
     PRD 3.5).
   - **`ConfiguracaoIaRequest/Response`** é decisão nova, não estava em
     nenhum ADR/doc anterior: como `ai-strategy.md` diz "cada usuário
     escolhe o seu [provedor]" (diferente do `document-service`, que usa
     um Ollama único pra todos via env var), precisa de algum lugar pra
     guardar essa escolha por usuário — nenhum outro serviço é dono
     desse dado, então fica no próprio `ai-service`. `apiKey` nunca é
     devolvida em texto claro, só se está `configurado`.
   - As tools MCP (tabela já documentada em `ai-strategy.md` seção 4)
     **não fazem parte deste contrato OpenAPI** — MCP é um protocolo
     próprio (JSON-RPC-like), não REST; a spec cobre só a superfície
     REST voltada ao cliente web/mobile.
   - `trace` na resposta do chat (lista de tools MCP chamadas) existe
     pra satisfazer o requisito de rastreabilidade do PRD seção 6 ("a IA
     deveria conseguir explicar de onde tirou o número").
2. ✅ Scaffold `services/ai-service/` (copiado de `document-service`:
   `pom.xml`, `mvnw`, Dockerfiles, `application.properties` porta 8086,
   `.gitignore`/`.dockerignore`, README). Sem MySQL/Flyway/Hibernate ORM
   no `pom.xml` — diferente de todos os outros serviços, `ai-service` não
   tem dado relacional próprio (só MongoDB + Qdrant, ver `overview.md`).
   Ajuste em relação ao plano original do item 1: cliente Qdrant **não**
   entrou no `pom.xml` ainda — fica pro item 7 (RAG), quando for
   realmente usado, mesmo critério já usado no scaffold do
   `document-service` (não adicionar dependência antes de ter código que
   a use). `application.properties` só com MongoDB + OIDC (sem service
   account, mesmo padrão do `budget-service`) + porta — os REST clients
   de saída (Ollama/OpenAI, e os quatro serviços das tools MCP) entram
   junto com a implementação de cada um (itens 5 e 6). Compilação
   validada (`./mvnw compile`). Ainda sem domínio/persistência/REST —
   próximo passo é o item 3.
3. ✅ Qdrant novo em `docker-compose.yml` (imagem `qdrant/qdrant:latest`,
   porta `6333` REST/dashboard + `6334` gRPC, volume `qdrant-data`).
   Validado subindo de verdade contra a stack real
   (`docker compose up -d qdrant` — start limpo, `GET /` respondeu 200
   com a versão, dashboard acessível). `README.md`: linhas novas na
   tabela de URLs (dashboard + REST/gRPC).
4. ✅ Domínio: `Conversa`/`Mensagem`/`AcaoPendente` (MongoDB), `ConfiguracaoIa`.
   `Conversa` é aggregate root com `Mensagem`/`AcaoPendente` **embutidos**
   (não em coleção própria) — diferente do split usado noutros agregados
   deste sistema (ex. `DocumentoImportado`/`LancamentoPendente` no
   document-service): faz sentido aqui porque `ai-service` só tem
   MongoDB, e "um documento por conversa" é o desenho natural do banco
   pra esse formato de dado. `AcaoPendente.propor()` calcula
   `expiraEm = agora + 10 minutos` (ADR-0007 só dizia "tempo curto", não
   um valor exato — decisão tomada agora);
   `Conversa.confirmarAcaoPendente(agora)` valida expiração e limpa o
   estado nos dois casos (sucesso ou expirada), nunca deixa uma proposta
   velha "pairando". `ConfiguracaoIa` segue o mesmo molde de `Reserva` no
   budget-service (1 documento por usuário, upsert, sem histórico) —
   `apiKey` no domínio é sempre texto plano (o domínio não sabe que
   criptografia existe); a persistência é quem criptografa/descriptografa
   na borda.
   Persistência: `CriptografiaService` novo (`infrastructure/security/`,
   AES-256/GCM, IV aleatório por chamada prefixado ao texto cifrado) —
   único jeito de guardar a `apiKey` da OpenAI conforme
   `security.md` já exigia ("campo criptografado"). Chave lida de
   `ai-service.criptografia.chave` (`AI_SERVICE_CRIPTOGRAFIA_CHAVE` em
   prod via Vault; default de dev gerado com `openssl rand -base64 32`,
   documentado como dev-only no `application.properties`).
   `ConfiguracaoIaRepositoryImpl` (não um mapper estático) é quem chama
   o `CriptografiaService` — mappers estáticos do projeto não têm acesso
   a bean CDI, então a criptografia fica no `RepositoryImpl`, que já é
   `@ApplicationScoped` com injeção de dependência.
   24 testes (`ConversaTest`, `AcaoPendenteTest`, `ConfiguracaoIaTest`,
   `CriptografiaServiceTest` — incluindo verificação de que o mesmo
   texto plano gera criptografados diferentes a cada chamada, por causa
   do IV aleatório). Sem teste de integração de banco ainda, mesmo
   padrão já usado nos outros serviços com Mongo (document-service item
   2, budget-service item 3) — fica pra ser exercida pelos testes REST
   do item 9.
5. ✅ Porta `LlmProvider` + adapters `OpenAiLlmProvider`/`OllamaLlmProvider`
   — cópia própria do `ai-service` (cada serviço tem a sua, ADR-0001,
   mesmo padrão de duplicação já visto em exceções tipo
   `ContaNaoEncontradaException` entre `card-service`/`transaction-service`/
   `document-service` — sem lib compartilhada entre microsserviços). Com
   `embed()` a mais em relação à porta do document-service (esse serviço
   precisa de RAG, aquele não precisava).

   Achado de design ao encaixar "cada usuário escolhe o seu provedor"
   (ADR-0002) na porta documentada em `ai-strategy.md` (que não tem
   parâmetro de config em `chat()`/`embed()`): criada
   `LlmProviderFactory` (porta nova, não estava em nenhum ADR) — resolve
   qual `LlmProvider` instanciar a partir da `ConfiguracaoIa` do usuário,
   uma vez por chamada. Isso manteve a porta `LlmProvider` exatamente
   como documentada (sem parâmetro de credencial), e tirou a variação de
   config de dentro dela. `OllamaLlmProvider`/`OpenAiLlmProvider` não são
   bean CDI (diferente de todo o resto do projeto) — são instanciados
   pela factory a cada chamada, carregando o estado resolvido
   (apiKey/modelo); os REST clients em si (`OllamaRestClient`/
   `OpenAiRestClient`) continuam sendo singletons CDI injetados na
   factory, já que suas base URLs são fixas (só a apiKey da OpenAI muda
   por chamada, passada como header). `ProvedorNaoConfiguradoLlmProvider`
   é um null object pra `ProvedorIa.NENHUM` — lança
   `IaNaoConfiguradaException` (422 na spec) se alguém tentar chamar
   `chat()`/`embed()` sem provedor configurado.

   Simplificação assumida conscientemente: `ConfiguracaoIa.ollamaUrl`
   (URL de Ollama customizada por usuário, já aceita pela spec) **ainda
   não é usada** pelo `OllamaLlmProvider` — sempre usa a instância
   default do `application.properties`. Suportar URL customizada por
   usuário exigiria construir o REST client dinamicamente por chamada
   (`RestClientBuilder` programático) em vez do client CDI fixo; fica
   pra quando isso virar necessidade real, não adiantar complexidade sem
   uso.

   6 testes novos (`LlmProviderFactoryImplTest` — dispatch por
   `ProvedorIa`, real lógica de negócio, testada; `ProvedorNaoConfiguradoLlmProviderTest`).
   Sem teste dedicado pros dois adapters (`OllamaLlmProvider`/
   `OpenAiLlmProvider`) — mesmo padrão já estabelecido no projeto (thin
   adapter HTTP, mockado nos testes de quem usa a porta `LlmProvider`,
   nunca testado contra o provedor real em CI, `testing-strategy.md`
   seção 4 — mesmo critério do `OllamaLlmProvider` do document-service).
   30 testes no total do `ai-service`, suite inteira verde.
6. ✅ Clientes de saída pra budget-service/card-service/transaction-service
   (implementação das tools MCP de leitura + `criar_transacao`, a única
   de escrita, PRD 3.5). **Sem `account-service`**, ajuste em relação ao
   plano original: nenhuma das quatro perguntas de exemplo do PRD 3.4
   precisa dele diretamente — `budget-service` já agrega saldo de conta
   no cálculo de disponível pra gastar (ADR-0026), então
   `buscar_saldo_disponivel` só chama `budget-service`
   (`GET /disponivel-para-gastar`, que já devolve o valor pronto).
   `BudgetServiceClient`/`CardServiceClient`/`TransactionServiceClient`
   (portas) + REST clients, mesmo padrão do `budget-service` (item 4
   daquela fatia): `PropagarAutorizacaoHeadersFactory` único
   compartilhado pelos três, nenhuma confirmação de posse (os cinco
   endpoints chamados já filtram pelo `sub` do token).
   `TransactionServiceClient.criarTransacao`/`criarTransacaoRecorrente`
   são os únicos métodos de escrita de toda a fatia — só devem ser
   chamados pelo agente orquestrador (item 8) depois de confirmação
   explícita do usuário (ADR-0007), a checagem disso é responsabilidade
   de quem chama, não do cliente. Sem teste dedicado pros três
   `*ClientImpl` — mesmo padrão já estabelecido (thin adapter, mockado
   nos testes de quem usa, itens 8/9). `./mvnw compile`/`test`
   validados, 30 testes continuam verdes (nada quebrou).
7. ✅ RAG: indexação de embeddings no Qdrant (descrição de transação) +
   busca semântica.

   Achado real que exigiu mudar `transaction-service` (fora do escopo
   original do item, mas necessário): `TransacaoRegistradaEvento`
   (publicado em `transacao.eventos` desde a fatia 1) **não tinha**
   `descricao`/`categoria` — só id/contaId/usuarioId/tipo/valor/data. Sem
   isso não tem o que indexar (RAG é sobre descrição, ai-strategy.md
   seção 2). Adicionados os dois campos ao evento e ao publisher —
   mudança aditiva segura: nenhum consumidor existia até agora pra esse
   tópico (confirmado por busca no código antes de mexer), suite de 102
   testes do `transaction-service` continua verde depois da mudança.

   Decisão de design nova, não prevista em nenhum ADR: **embedding
   sempre usa Ollama local, independente do provedor de chat escolhido
   pelo usuário** (`LlmProviderFactory.criarParaEmbedding()`, método
   novo). Motivo: modelos de embedding diferentes geram vetores de
   dimensão diferente (`nomic-embed-text` do Ollama = 768,
   `text-embedding-3-small` da OpenAI = 1536), e uma coleção do Qdrant
   tem dimensão fixa — se cada usuário indexasse com o vetor do seu
   próprio provedor de chat, a coleção quebraria na primeira busca sobre
   dado de outro usuário. Consequência prática boa: indexação não
   depende de `ConfiguracaoIa` nenhuma — toda transação é indexada
   assim que é criada, mesmo que o usuário nunca tenha configurado IA
   ainda (dado já fica pronto pra quando ele configurar).

   Pipeline: `transaction-service` publica `transacao.eventos` →
   `TransacaoRegistradaConsumer` (primeiro consumer desse tópico) →
   `IndexarTransacaoUseCase` (embed via Ollama + `VectorStore.indexar`).
   `BuscarTransacoesSimilaresUseCase` é o lado de consulta (embed da
   pergunta do usuário + `VectorStore.buscarSimilares`, filtrado por
   `usuarioId` sempre — isolamento multi-tenant, ADR-0003), usado pela
   tool `buscar_transacoes` no item 8.

   `QdrantVectorStoreImpl` fala REST puro com o Qdrant (sem client Java
   dedicado — mesmo critério de "REST client interface" já usado com
   Ollama/OpenAI/todos os outros serviços; nenhum client oficial
   verificado, contrato validado na mão contra o container real).
   `QdrantColecaoInicializador` cria a coleção no startup se não existir
   (idempotente, `GET /collections/{nome}` 404 → `PUT` cria). Contrato
   da API (criar coleção, upsert de ponto, busca com filtro por
   `usuarioId`, 404 de coleção inexistente) validado na prática com
   `curl` direto contra o container Qdrant já rodando (item 3) — request/
   response batem exatamente com os DTOs escritos.

   9 testes novos (`IndexarTransacaoUseCaseTest`,
   `BuscarTransacoesSimilaresUseCaseTest`, `TransacaoRegistradaConsumerTest`
   — mapeamento evento→comando, mesmo padrão do
   `DocumentoLancamentosConfirmadosConsumerTest` do transaction-service —,
   `LlmProviderFactoryImplTest` ganhou um caso novo pro
   `criarParaEmbedding()`). Sem teste dedicado pro `QdrantVectorStoreImpl`
   (thin HTTP adapter, mesmo critério já estabelecido — validado hoje
   via `curl` real em vez de mock, fica pra um teste de integração
   `@QuarkusTest` se um dia for necessário). 34 testes no total do
   `ai-service`, suite inteira verde.
8. ✅ Agente orquestrador (`AgenteOrquestradorUseCase`): detecção de
   intent, escolha de tool, fluxo de confirmação obrigatória (ADR-0007).
   Único caso de uso pra tudo que acontece na conversa — mesmo desenho
   já decidido na spec do item 1 (`POST /chat` único endpoint).

   Achado real que exigiu mexer no item 4 (não previsto no plano
   original): `AcaoPendente` não tinha campo `descricao`, mas
   `criar_transacao` do transaction-service exige esse campo — sem ele,
   a ação confirmada não tinha o que executar. Adicionado
   `AcaoPendente.descricao` (domínio + persistência + testes do item 4,
   tudo revalidado depois).

   Decisões de design tomadas ao montar o fluxo:
   - **Confirmação por palavra-chave, não por LLM.** Detectar "sim"/
     "confirmo"/"ok" contra um conjunto fixo de frases (case-insensitive,
     substring) é mais simples e mais confiável que outra chamada ao
     LLM só pra essa decisão binária — economiza uma chamada e um ponto
     de falha (JSON malformado) por confirmação.
   - **Correção = tratar como comando novo, não como "diff" da proposta
     anterior.** `ai-strategy.md` seção 4.2 descreve um fluxo de
     "atualiza a proposta" quando o usuário corrige; na prática, limpar
     a proposta antiga e reclassificar a mensagem nova do zero produz o
     mesmo resultado observável (nova proposta correta) com bem menos
     estado pra gerenciar. Documentado como simplificação consciente,
     não um desvio silencioso.
   - **`descricao` sempre extraída do LLM, `contaId` nunca.** Conta é
     texto livre ("conta corrente", "carteira") resolvido em código
     Java determinístico contra `AccountServiceClient.buscarContasAtivas()`
     (match por substring nos dois sentidos) — nunca um UUID inventado
     pelo LLM. Sem match ou sem menção de conta, o agente pergunta "qual
     conta devo usar?" (RESPOSTA, não propõe nada ainda) — implica que
     `account-service` voltou a ser cliente deste serviço (tinha sido
     descartado no item 6, que só olhava as tools de leitura — a
     necessidade real apareceu aqui, no fluxo de escrita).
   - **Resposta final da consulta é sempre montada em Java, nunca pelo
     LLM.** O número exato (saldo, gasto) vem de tool call determinístico
     e é formatado por template — zero risco do LLM "reformular" e errar
     o valor na resposta final (PRD: "nunca inventada"). O LLM só decide
     *qual* tool chamar e *quais* parâmetros extrair, nunca o texto final
     com o número.
   - **Bug real encontrado escrevendo o teste, corrigido antes de
     commitar**: a condição original só tentava confirmar quando
     `temAcaoPendenteValida()` — o que excluía justamente o caso de uma
     proposta expirada, que caía direto num "não entendi" genérico em
     vez de avisar "sua proposta expirou". Corrigido pra checar só a
     presença da ação (`getAcaoPendente() != null`) e deixar
     `Conversa.confirmarAcaoPendente` decidir validade — agora o catch de
     `AcaoPendenteExpiradaException` é alcançável de verdade (era código
     morto antes).

   **Prompts testados de verdade contra o Ollama real** (não só em
   teoria — mesmo cuidado que o `AgenteExtracaoFaturaService` do
   document-service teve, `docs/historico.md` 2026-08-09), via `curl`
   direto: classificação de intent acertou de primeira (pergunta de
   saldo → `buscar_saldo_disponivel` + `MES_ATUAL`); extração de ação
   inicialmente confundiu `descricao` com o texto da conta ("na conta
   corrente" virou `descricao: "Conta Corrente"`) — prompt corrigido
   (instrução explícita "NUNCA repita esse valor em descricao") e
   reveridicado, passou a extrair "Aluguel" (um palpite razoável pro
   contexto, já que o comando de teste não mencionava descrição
   nenhuma — aceitável, é exatamente pra isso que existe a confirmação,
   ADR-0007: usuário vê o resumo antes de qualquer coisa persistir).
   Pergunta comparativa ("gastei mais que o mês passado?") fez o modelo
   devolver um período inválido (`"MES_ATUAL,MES_PASSADO"`, dois valores
   concatenados) — o parsing já tinha fallback pra isso
   (`PeriodoReferencia` desconhecido → `MES_ATUAL`), então não quebrou;
   aproveitado o achado pra melhorar `responderResumoCategoria` a somar
   `totalGastoPeriodoAnterior` (campo que o transaction-service já
   calculava, mas a resposta não usava) e responder "maior"/"menor"
   diretamente, sem pedir aritmética ao LLM (mesma lição do
   document-service).

   16 testes novos em `AgenteOrquestradorUseCaseTest` cobrindo as quatro
   tools de consulta, proposta de ação com conta resolvida, pedido de
   conta quando não identificada, extração incompleta, intent
   desconhecida, confirmação de ação pontual e recorrente, proposta
   expirada, correção limpando a proposta anterior, e isolamento de
   conversa por usuário (404 se não for sua). 51 testes no total do
   `ai-service`, suite inteira verde.
9. ✅ REST (`ChatResource`, `ConversaResource`, `ConfiguracaoResource`) +
   testes de integração. DTOs REST com `static de(...)`, mesmo molde do
   resto do projeto — `ChatResponse`/`ChatRequest` (REST) coexistem sem
   colisão com `ChatResponse`/`ChatRequest` do domínio (usados só pra
   falar com o `LlmProvider`) porque vivem em pacotes diferentes
   (`infrastructure.rest.dto` vs `domain`) e a camada REST nunca importa
   os do domínio diretamente. Spec ganhou o campo `descricao` em
   `AcaoProposta` (não previsto originalmente — só apareceu quando o
   domínio ganhou esse campo no item 8).

   Casos de uso novos, pequenos, seguindo o padrão já estabelecido
   (`BuscarCartaoUseCase`/`BuscarReservaUseCase`): `ListarConversasUseCase`,
   `BuscarConversaUseCase` (404 anti-IDOR), `BuscarConfiguracaoIaUseCase`
   (nunca 404), `DefinirConfiguracaoIaUseCase` (upsert, mesmo molde de
   `DefinirReservaUseCase` do budget-service).

   Achado real rodando os testes de integração pela primeira vez: o REST
   client do Quarkus lança `org.jboss.resteasy.reactive.ClientWebApplicationException`
   pra qualquer status de erro, **não** `jakarta.ws.rs.NotFoundException`
   especificamente — `QdrantColecaoInicializador` (item 7) tinha um
   `catch (NotFoundException e)` que nunca disparava de verdade (só não
   tinha sido exercido antes porque nada subia o `@QuarkusTest` completo
   até este item). Corrigido pra `catch (WebApplicationException e)` +
   checar `e.getResponse().getStatus() == 404` manualmente — mais
   robusto, não depende de qual subtipo exato de exceção o client decide
   lançar.

   Exception mapper novo: `IllegalArgumentExceptionMapper` (400) — cobre
   validação de campo cruzado que o Bean Validation da request não
   expressa sozinho (`apiKey` obrigatória só se `provedor=OPENAI`,
   validado no domínio via `ConfiguracaoIa.validarProvedor`, não na
   anotação da request).

   13 testes de integração novos (`ChatResourceTest` — consulta de
   ponta a ponta com todos os clientes mockados via `QuarkusMock`, 400
   de mensagem vazia, 404 de conversa inexistente, 401 sem token;
   `ConversaResourceTest` — cria a conversa via `POST /chat` de verdade
   (não existe endpoint de criação direta), lista, busca histórico
   completo, isolamento por usuário; `ConfiguracaoResourceTest` —
   nunca configurado, define Ollama sem apiKey, define OpenAI e confirma
   que `apiKey` nunca aparece na resposta, 400 sem apiKey pra OpenAI).
   64 testes no total do `ai-service`, suite inteira verde.
10. ✅ `docker-compose.yml`/CI/Keycloak/Postman/diagramas (fechamento da
   fatia, mesmo padrão do item 7 do `budget-service`). `docker-compose.yml`:
   serviço `ai-service` novo, `depends_on` mongo/qdrant/kafka/ollama +
   account/budget/card/transaction-service, porta 8086. Build+subida
   validados de verdade contra a stack real (`./mvnw package -DskipTests`
   + `docker compose up -d --build ai-service` — build limpo, healthcheck
   200, `QdrantColecaoInicializador` confirmou "coleção já existe" em vez
   de recriar, Mongo/Kafka conectados). CI: job `ai-service` novo em
   `ci.yml` (mesmo template dos outros), `ai-service` no filtro de paths
   do job `changes`. Keycloak: client `ai-service` registrado — **sem**
   service-account (mesmo padrão do `budget-service`/`document-service`):
   `ai-service` só propaga o token do usuário pros serviços que chama,
   nunca via client_credentials (aplicado também no container Keycloak
   já rodando — testado emitindo token real e chamando `GET /configuracao`
   e `GET /conversas`, ambos 200). Postman: 5 endpoints novos documentados
   em `mudancas-manuais.txt` (com exemplo de body pra cada fluxo do chat
   — consulta/ação/confirmar/corrigir) + variável `ai_service_url` no
   environment. `diagrams.md`: container graph sem mais "(planejado)"
   no `ai-service` nem "proposto" no Qdrant, arestas síncronas novas
   (account/budget/card/transaction-service) + aresta Kafka
   (`transacao.eventos` → `ai-service`, indexação RAG), seção 3.6 nova
   (ER conceitual de `Conversa`/`Mensagem`/`AcaoPendente`/`ConfiguracaoIa`),
   seção 4.7 nova (diagrama de classes do domínio, incluindo os ports
   `LlmProvider`/`LlmProviderFactory`/`VectorStore`), índice de fluxos
   (seção 6) atualizado. `overview.md`: tabela de serviços → ✅ Entregue,
   seção 4 reescrita pra refletir a implementação real (endpoint único,
   confirmação por palavra-chave, resposta de consulta sempre por
   template Java) + subseção 4.3 nova (fluxo de indexação RAG via Kafka).
   `roadmap.md`: fatia 5 → ✅ Entregue, próxima ação aponta pra fatia 6
   (front-end). `README.md` raiz: parágrafo do `ai-service` no "Estado
   atual", tabela de endpoints, URLs (REST/Swagger/MongoDB), contagem de
   serviços no CI/docker compose atualizada de cinco pra seis, nota de
   MCP no bullet de stack ajustada pra refletir a implementação real
   (agente orquestrador com tools internas, não protocolo MCP exposto de
   fato — simplificação consciente vs. `ai-strategy.md`, não revisitada
   nesta fatia).

**Fatia 5 (`ai-service`) entregue.** Chat com agente de IA — RAG (Qdrant +
Ollama/OpenAI), consulta e ação (criação de transação) por linguagem
natural, confirmação obrigatória antes de qualquer ação (ADR-0007). 64
testes, CI verde, container validado contra a stack real (Mongo/Kafka/
Qdrant conectados, endpoints autenticados testados via token real).

## Fatia 6 — Front-end Next.js

Primeira interface visual do sistema (PRD, roadmap #6) — até aqui tudo foi
validado via Swagger/Postman. Cobre dashboard com gráfico de gastos por
categoria (PRD 3.7), CRUD completo de conta/transação, upload de
documento e chat com a IA, todos consumindo os seis serviços já
entregues. Next.js assume o papel de BFF (ADR-0006) — Route
Handlers/Server Components agregam os microsserviços, nenhum serviço
"gateway" novo.

Três decisões em aberto desde o ADR-0006 foram fechadas antes de começar
(confirmadas pelo usuário, 2026-08-10):
- **Estilo/componentes**: Tailwind CSS + shadcn/ui (componentes copiados
  pro repo, não dependência de runtime — zero lock-in, fácil de temizar
  claro/escuro depois).
- **Autenticação**: Auth.js (NextAuth v5) com provider OIDC genérico
  contra o Keycloak, reaproveitando o client `web-app` já existente —
  ver ADR-0027 (regra completa: sessão JWT em cookie httpOnly, access
  token nunca exposto ao client-side, refresh automático, logout também
  encerra a sessão SSO no Keycloak).
- **Direção visual inicial**: paleta neutra/profissional — base cinza +
  um azul de destaque — ajustável depois que o usuário ver o produto
  rodando, não é uma identidade final fechada em pedra.

Testes seguem `docs/architecture/testing-strategy.md` seção 3 (já
decidido antes desta fatia): Vitest + React Testing Library pra
componente/hook, MSW pra mockar chamada HTTP na borda (nunca mockar a
função que faz a chamada).

1. ✅ Scaffold do projeto (`services/web/` — nome de pasta segue o padrão
   `services/<nome>/` do resto do repo, mesmo não sendo um serviço
   Quarkus): `create-next-app` (Next.js 16, App Router, Turbopack,
   TypeScript, Tailwind CSS v4), init do shadcn/ui (`baseColor: neutral`
   — já bate com a decisão de paleta), tokens de cor claro/escuro
   editados em `app/globals.css` pra trocar `--primary`/`--ring`/
   `--sidebar-primary` de cinza puro pra azul de destaque
   (`oklch(0.546 0.185 259.8)` claro / `oklch(0.65 0.16 259.8)` escuro),
   resto da paleta mantido neutro. `next.config.ts` com
   `output: "standalone"`. Tooling de teste (Vitest + React Testing
   Library + MSW, `testing-strategy.md` seção 3) — smoke test da página
   inicial passando. `Dockerfile` multi-stage (builder `npm ci` + `npm
   run build`, runner só copia `.next/standalone` + estáticos, usuário
   não-root) validado de ponta a ponta: `npm run lint`/`npm run test`/
   `npm run build` limpos, `docker build` + `docker run` respondendo
   200 em `GET /`.

   Achado real: `npx shadcn@latest init` adicionou o próprio pacote
   `shadcn` (CLI) como `dependencies` em vez de `devDependencies` —
   corrigido na mão (é uma ferramenta de build, nunca importada em
   runtime). Isso causou um conflito de peer dependency ao instalar
   `@vitejs/plugin-react` (Babel 7 do `shadcn` vs. peer opcional em
   Babel 8 de uma dependência do Vite/rolldown) — resolvido com
   `--legacy-peer-deps` (seguro aqui, conflito é só entre ferramentas de
   dev, não afeta runtime). `next dev`/`next build` do Next.js 16 têm
   mudanças reais de convenção em relação a versões anteriores (ex:
   `proxy.ts` substitui `middleware.ts`) — guias completos ficam em
   `node_modules/next/dist/docs/`, consultados antes de decidir a
   estrutura (relevante principalmente pro item 2, autenticação).
2. ✅ Autenticação (ADR-0027): Auth.js (NextAuth v5 beta) com o provider
   Keycloak nativo (`next-auth/providers/keycloak`), client `web-app`
   reaproveitado como está (público, sem `client_secret`) —
   `client: { token_endpoint_auth_method: "none" }` no provider evita o
   Auth.js tentar enviar Basic Auth com secret `undefined`; PKCE + state
   já são o default do Auth.js pra qualquer provider OIDC, então o fluxo
   continua seguro sem secret. `proxy.ts` (não `middleware.ts` — Next.js
   16 renomeou, ver item 1) com `callbacks.authorized` protegendo toda
   rota exceto `/login` e `/api/auth/*`, redirecionando pra `/login`
   automaticamente. Sessão JWT (cookie httpOnly) — `callbacks.jwt`
   guarda `accessToken`/`refreshToken`/`idToken`/`expiresAt`, renova
   sozinho quando expira (`lib/auth-token-refresh.ts`, funções puras
   testadas: `precisaRenovar`/`renovarToken`, 7 testes). `callbacks.session`
   NUNCA expõe token nenhum — só `user.id`/`name`/`email` — porque esse
   objeto é literalmente o que `/api/auth/session` devolve pra qualquer
   fetch do navegador (mesmo em Server Component); Route Handler que
   precisar do access token usa `lib/auth-token.ts`
   (`getToken()`, lê o cookie httpOnly direto, nunca passa pelo endpoint
   público). Logout (`lib/auth-actions.ts`) faz RP-Initiated Logout de
   verdade: limpa a sessão local E redireciona pro
   `end_session_endpoint` do Keycloak com `id_token_hint`, encerrando a
   sessão SSO — não só um "logout de mentirinha" que deixa o Keycloak
   ainda logado.

   **Achado crítico, pego lendo o código-fonte do `@auth/core` antes de
   confiar cegamente**: sem adapter de banco configurado (é o nosso
   caso, sessão é só JWT), o Auth.js gera um `user.id` **aleatório**
   (`crypto.randomUUID()`) a cada login — não usa o `sub` do id_token
   por padrão. Se eu não tivesse sobrescrito isso, `session.user.id`
   nunca bateria com o `usuarioId` usado em todo o resto do sistema
   (ADR-0003), quebrando silenciosamente qualquer chamada aos
   microsserviços feita em nome do usuário. Corrigido no `callbacks.jwt`:
   `token.sub = profile.sub` (o `profile` cru do id_token, disponível só
   no login inicial). **Validado de ponta a ponta contra o Keycloak
   real** (login completo via `curl` simulando o browser — GET da tela
   de login, POST de usuário/senha, follow do callback OAuth): a sessão
   final trouxe `user.id = "cd4cf57c-b5a8-4b2c-b9b5-ffba5770e19d"`,
   exatamente o mesmo `sub` que o `usuario.teste` já usava em todo teste
   anterior deste projeto (account-service, ai-service, etc.) — confirma
   que o fix realmente resolve o problema, não só na teoria.

   `.env.example` novo (`AUTH_SECRET`, `AUTH_TRUST_HOST`, `AUTH_URL`,
   `AUTH_KEYCLOAK_ID`, `AUTH_KEYCLOAK_ISSUER`) — `AUTH_SECRET` de dev
   gerado com `openssl rand -base64 33`, registrado no inventário de
   credenciais (`security.md`). `app/login/page.tsx` (form com Server
   Action `entrar()`, chama `signIn("keycloak")`) e `app/page.tsx`
   atualizado (mostra usuário logado + botão sair — placeholder até o
   item 3 construir o shell de verdade). `types/next-auth.d.ts` faz
   module augmentation do `Session`/`JWT` pros campos customizados.
3. ✅ Layout base (shell): route group `app/(app)/` (não muda URL, só
   agrupa layout — `/login` fica fora, sem cabeçalho/nav) com
   `layout.tsx` novo: cabeçalho (nome do sistema, navegação principal,
   usuário logado + botão sair). Página inicial (antigo `app/page.tsx`)
   movida pra `app/(app)/page.tsx` e virou o placeholder do "Dashboard"
   (item 6) — texto de "logado como"/botão sair saíram de lá porque o
   cabeçalho já cobre isso. `lib/nav-items.ts` centraliza os itens de
   menu com uma flag `implementado`: item ainda não construído (Contas,
   Transações, Documentos, Chat IA — itens 4/5/7/8) aparece no menu como
   texto "em breve", não como link — evita 404 clicando em algo que
   ainda não existe. Layout confirma sessão de novo
   (`if (!sessao?.user) redirect("/login")`) mesmo já protegido pelo
   `proxy.ts`, defensivo pro caso de renovação de token falhar
   (`sessao.error`), com aviso visível no cabeçalho nesse caso. `npm run
   lint`/`test`/`build` limpos, hot reload do Turbopack confirmou sem
   precisar reiniciar o dev server.
4. ✅ CRUD de conta (`account-service`): listar/criar/editar/excluir —
   primeira tela de dado real. Padrão que os itens seguintes (5, 7, 8)
   reaproveitam: `lib/account-service.ts` (client HTTP server-only,
   `obterAccessToken()` do item 2 propaga `Authorization: Bearer`,
   `cache: "no-store"` — dado por usuário nunca pode vazar entre
   requests/usuários diferentes), Server Component (`app/(app)/contas/page.tsx`)
   busca a lista direto (sem Route Handler intermediário — decisão
   tomada na hora, ver nota abaixo), Server Actions
   (`app/(app)/contas/actions.ts`: `criarContaAction`/`atualizarContaAction`/
   `excluirContaAction`, com `revalidatePath("/contas")` depois de cada
   mutação) + client component com `useActionState` pro formulário
   (`conta-form-dialog.tsx`, dialog reutilizado pra criar E editar).

   **Desvio consciente do que o item previa**: a doc oficial do Next.js
   (lida antes de escrever código, `node_modules/next/dist/docs/.../mutating-data.md`)
   deixa claro que Server Actions são o caminho recomendado pra mutação
   no App Router — Route Handler vira proxy só quando o cliente
   realmente precisa de um endpoint HTTP de verdade (ex: chamado de
   fora do React, como webhook). Como toda mutação aqui parte de um
   `<form>` da própria UI, Server Action é mais simples (sem
   fetch/serialização manual no client, `useActionState` já dá
   pending/erro de graça) e é o padrão que os itens 5/7/8 vão seguir —
   por isso description original do item ("Route Handler") não reflete
   o que foi construído.

   `lib/nav-items.ts`: "Contas" virou `implementado: true`.

   Validado contra a stack real (não só mock): GET `/contas` autenticado
   (mesmo fluxo de login simulado via `curl` do item 2) trouxe as 10
   contas reais que `usuario.teste` já tinha de testes anteriores desta
   sessão — confirma que `obterAccessToken()`/propagação de token/parse
   de resposta funcionam de ponta a ponta contra o `account-service` de
   verdade. Mutação (criar/editar/excluir) validada só via os testes
   automatizados (`account-service.test.ts`, 5 testes com `fetch`
   mockado) + `npm run build`/`lint` — não dá pra simular um Server
   Action via `curl` puro (protocolo Flight do React exige um header
   `Next-Action` com encoding específico), então o clique de verdade no
   formulário fica pro usuário conferir no navegador.
5. ✅ CRUD de transação (`transaction-service`): mesmo padrão do item 4
   (`lib/transaction-service.ts` server-only, Server Component pra
   leitura, Server Actions pra mutação). `/transacoes` tem duas seções
   na mesma página: transações (filtro por conta/período via `<form
   method="GET">` nativo — sem JS nenhum, `searchParams` é lido direto
   no Server Component; criar/editar via dialog; cancelar reverte
   saldo) e regras recorrentes (criar/listar/cancelar — sem editar,
   API não tem PUT pra regra recorrente). Frequência fixa em `MENSAL`
   no form (único valor aceito no v1, mesma regra do `transaction-service`).

   Extraído `components/confirm-action-button.tsx` (form com
   `confirm()` nativo antes de submeter) — segunda vez que esse padrão
   apareceu (excluir conta no item 4, cancelar transação/regra aqui),
   hora de eliminar a duplicação; `excluir-conta-button.tsx` do item 4
   refatorado pra usar o componente novo também.

   Filtro de conta/período optou por `<select>`/`<input type=date>`
   nativos em vez do `Select` do shadcn/ui — é só uma barra de filtro
   simples via GET, não precisa de hidratação client-side nenhuma pra
   funcionar (funciona até sem JavaScript no navegador).

   Validado contra a stack real: GET `/transacoes` autenticado trouxe
   as 10 transações e 3 regras recorrentes reais que `usuario.teste` já
   tinha; filtro `?contaId=...` testado também (200, mesmo padrão de
   login simulado via `curl` dos itens 2/4). 6 testes novos
   (`transaction-service.test.ts`) cobrindo montagem de query string,
   erro 401 sem token, e propagação de mensagem/status de erro
   (incluindo 422 de saldo insuficiente).
6. ✅ Dashboard (PRD 3.7), `app/(app)/page.tsx` (substitui o
   placeholder do item 3): `lib/budget-service.ts` novo (mesmo padrão
   server-only dos itens 4/5), `resumoPorCategoria` adicionado a
   `lib/transaction-service.ts`. Seletor de mês (`<input type="month">`
   — casa exatamente com o formato AAAA-MM que budget-service/
   transaction-service esperam, sem precisar converter nada) num
   `<form method="GET">` nativo, mesmo espírito do filtro do item 5.
   `lib/mes.ts` (`mesAtual`/`limitesDoMes`) deriva `inicio`/`fim` do mês
   selecionado pra alimentar `resumo-por-categoria` (que trabalha com
   data, não mês) — 4 testes cobrindo mês de 30/31 dias e fevereiro
   bissexto/não bissexto.

   Gráfico de gastos por categoria: componente oficial `chart` do
   shadcn/ui (Recharts por baixo) — os tokens `--chart-1`.._-5` já
   tinham sido definidos no item 1 (scaffold), sem precisar mexer em
   `globals.css` de novo. Barra dupla por categoria (`totalGasto` vs
   `totalGastoPeriodoAnterior`, PRD 3.7 pede "comparação com o período
   anterior" — o `transaction-service` já calcula isso, só precisava
   plotar). "Disponível pra gastar" mostra o valor + o detalhamento
   item a item que o `budget-service` devolve (saldo de contas, faturas
   em aberto, despesas recorrentes, reserva) — mesma filosofia de
   auditoria/explicação do ADR-0026. Reserva: form pequeno inline
   (`reserva-form.tsx`), não é lista, é valor único por usuário.
   Orçamentos: mesmo padrão lista+dialog dos itens 4/5
   (`orcamento-form-dialog.tsx`, reutiliza `ConfirmActionButton` do
   item 5 pra cancelar) — editar só manda `valorLimite` (único campo
   editável na API), categoria/mês ficam fixos depois de criado.

   Validado contra a stack real: GET `/` autenticado trouxe o
   "disponível pra gastar" real (R$ 12.340,00, 9 contas somadas) e o
   payload do gráfico com `categoria`/`totalGasto` de verdade — mesmo
   roteiro de login via `curl` dos itens 2/4/5. 4 testes novos em
   `budget-service.test.ts` (401 sem token, querystring do mês, 422 de
   orçamento duplicado, corpo do PUT da reserva) + os 4 de `mes.test.ts`
   — 27 testes no total do `web` agora.

   **Extensão pedida depois do item 7** (2026-08-10): duas
   funcionalidades novas no dashboard, fora do escopo original do
   backlog.
   - **Reserva sugerida**: regra decidida com o usuário (via pergunta
     direta — não é ambiguidade pequena, é regra de negócio nova) =
     média de RECEITA confirmada dos últimos 3 meses (`MESES_MEDIA_RECEITA`
     em `lib/reserva-sugerida.ts`, função pura `calcularReservaSugerida`,
     4 testes). `lib/mes.ts` ganhou `limitesUltimosMeses(mesReferencia,
     quantidade)` pra calcular a janela (3 testes, incluindo virada de
     ano). `ReservaForm` ganhou um link "usar sugestão" que só preenche
     o campo (client-side, sem round-trip ao servidor — o valor já veio
     calculado do Server Component).
   - **Cotação do dólar**: `lib/cambio.ts`, integração nova com a
     AwesomeAPI (`economia.awesomeapi.com.br`, pública, sem chave —
     decidida com o usuário). Primeira chamada HTTP do projeto que NÃO
     é a um dos seis microsserviços — sem `obterAccessToken()`, sem
     `cache: "no-store"` (dado público, igual pra todo usuário,
     cacheado 5min via `next: { revalidate: 300 }`, diferente de todo
     outro client HTTP do projeto que é por-usuário e nunca pode
     cachear). Falha da API externa não derruba o dashboard — captura
     erro e mostra "cotação indisponível", 3 testes cobrindo sucesso,
     resposta não-ok e falha de rede.

   Validado contra a stack real e a API externa de verdade: GET `/`
   trouxe a cotação real do dia (~R$ 5,11) via AwesomeAPI e a reserva
   sugerida bateu exatamente com o cálculo manual (1 receita confirmada
   de R$ 5.000 na janela → sugestão R$ 1.666,67). 42 testes no total do
   `web` agora.
7. ✅ Upload de documento (`document-service`): `lib/document-service.ts`
   (mesmo padrão server-only, mas com uma diferença real — upload é
   `multipart/form-data`, não JSON. `Content-Type` **não** é setado na
   mão: precisa do boundary que o `fetch` calcula sozinho a partir do
   `FormData`; setar manualmente quebraria o multipart. Único client
   HTTP dos quatro que tem essa exceção documentada no próprio código).

   Fluxo em três telas: `/documentos` (form de upload + lista, mesmo
   padrão lista dos itens 4-6) → upload aceito redireciona pra
   `/documentos/{id}` (a spec do `document-service` já é assíncrona —
   extração real leva minutos, não segundos, testado na prática desde a
   fatia 3) → `/documentos/[id]` faz **polling client-side** enquanto
   status é `RECEBIDO`/`PROCESSANDO` (`setInterval` chamando uma Server
   Action a cada 4s, não é uma Server Action tradicional ligada a
   formulário — é chamada direto do `useEffect`, mesmo padrão que a doc
   oficial do Next.js mostra pra "refresh data" fora de mutação de
   form) até virar `AGUARDANDO_CONFIRMACAO` (mostra os lançamentos com
   checkbox — todos marcados por padrão, desmarcar = rejeitar — + select
   de conta) ou `ERRO_PROCESSAMENTO` (mostra `mensagemErro`).

   `next.config.ts`: `experimental.serverActions.bodySizeLimit` subido
   de `1mb` (default do Next.js) pra `10mb` — casa com
   `quarkus.http.limits.max-body-size=10M` que o `document-service` já
   aceita; sem isso, fatura PDF real (testada com fatura de banco
   grande na fatia 3) estouraria o limite de Server Action antes de
   sequer chegar no back-end.

   **Limite de validação, registrado com transparência**: diferente dos
   itens 4-6 (onde pelo menos o GET foi validado contra a stack real
   via `curl` simulando login), o upload em si passa por uma Server
   Action com `<input type="file">` — o protocolo Flight do React pra
   isso é ainda mais específico que uma Server Action comum (multipart
   com boundary próprio, action ID de build), não dava pra reproduzir
   de forma confiável via `curl` no tempo desta sessão. Validado via 5
   testes automatizados (`document-service.test.ts` — inclusive um
   teste específico que confirma o `Content-Type` NÃO é setado na mão
   antes do `fetch`) + `npm run build`/`lint` limpos + GET `/documentos`
   confirmado contra o `document-service` real (0 documentos do
   `usuario.teste` até agora). O upload real de PDF (há três faturas de
   teste em `test-data/` já usadas na fatia 3) fica pro usuário
   testar clicando na tela — é o item com menos cobertura automatizada
   da fatia até aqui, vale atenção extra na conferência manual.
8. ✅ Refinamento visual (design system) — inserido a pedido do
   usuário, que trouxe um prompt extenso pedindo um "design system
   completo" (40+ componentes, docs exaustivas) inspirado em
   Stripe/Vercel/Linear/Notion/Apple e referências de produto (YNAB,
   Monarch Money, Copilot Money etc.). Antes de codar, análise crítica
   apresentada ao usuário (não implementei calado): o pedido original
   contradiz o princípio de fatias verticais do `CLAUDE.md` (construir
   componente sem uso real, ex: Command Palette/Data Grid/Timeline) e
   propõe páginas de menu sem serviço de back-end correspondente
   (Metas, Investimentos dedicado, Relatórios, Importações,
   Exportações). Usuário concordou com a versão enxuta em 3 decisões
   (perguntadas via `AskUserQuestion`): doc viva e incremental (não
   especificação antecipada), trocar fonte Geist→Inter, e fazer esse
   refinamento como item dedicado antes do chat de IA (por isso o chat
   virou item 9 e o fechamento item 10).

   Entregue: `docs/architecture/design-system.md` novo (tokens
   realmente implementados — cor, tipografia, spacing, radius, shell,
   padrões de página já estabelecidos — e uma seção explícita do que
   ficou de fora e por quê). Tokens: `--radius` 10px→12px
   ("nunca cantos retos"), `Card` ganhou `shadow-sm` + borda mais
   discreta (`ring-foreground/5`, era `/10`). Fonte: Geist→Inter
   (`app/layout.tsx`) — **achado real**: a variável do Geist se
   chamava `--font-geist-sans`, mas o `@theme inline` (herdado do init
   do shadcn/ui) sempre referenciou `--font-sans` — nomes diferentes,
   então a fonte customizada nunca esteve de fato aplicada em nenhuma
   tela dos itens 1-7 (fallback silencioso pro padrão do navegador);
   corrigido nomeando a variável do `Inter()` exatamente `--font-sans`.
   `--font-mono` (apontava pro Geist Mono, removido) trocado pra cair
   no monospace default do Tailwind.

   Shell: header horizontal (item 3) trocado por menu lateral fixo
   (desktop) / off-canvas (mobile) — `app/(app)/app-sidebar.tsx` novo,
   `app/(app)/layout.tsx` reescrito. Decisão consciente de **não** usar
   o bloco "sidebar" completo do shadcn/ui (collapse-to-icon, atalho de
   teclado, cookie de persistência, tooltip por item) — infraestrutura
   de dashboard enterprise sem uso real pros 5 itens de menu atuais,
   mesmo raciocínio aplicado à crítica do pedido original. Escrito na
   mão: `useState` só pro estado mobile, `md:` breakpoint CSS puro pro
   desktop (sem hook de detecção de mobile — evita flash de layout
   errado no primeiro paint), item ativo via `usePathname()`.

   Padding de página: `p-6` → `p-6 md:p-8` nas 5 páginas existentes
   (contas/transações/dashboard/documentos/documentos detalhe) — mais
   respiro no desktop, mobile inalterado.

   Validado contra a stack real (mesmo roteiro de login via `curl` dos
   itens anteriores): shell novo confirmado renderizando (aria-labels
   "Abrir menu"/"Fechar menu" presentes na resposta), fonte Inter
   confirmada carregando. `npm run lint`/`test`(42)/`build` limpos —
   mudança é puramente visual/estrutural, sem lógica nova, então sem
   testes novos.
9. ✅ Chat com a IA (`ai-service`): `lib/ai-service.ts` (mesmo padrão
   server-only) + `app/(app)/chat/` — layout com lista de conversas
   (sidebar, `GET /conversas`) e botão de configuração de provedor;
   `/chat` (conversa nova) e `/chat/[id]` (conversa existente, `GET
   /conversas/{id}`) renderizam o mesmo `ChatClient`.

   Diferente dos itens 4-7 (Server Action + `useActionState` presa a
   `<form>`), o envio de mensagem precisa de uma lista que cresce a
   cada troca — `ChatClient` é client component com `useState` local
   pra lista de mensagens, `enviarMensagemAction` chamada direto (não
   presa a form), mensagem do usuário aparece otimisticamente antes da
   resposta chegar. Primeira mensagem de uma conversa nova atualiza a
   URL pra `/chat/{conversaId}` via `router.replace()` (sem reload) —
   dá pra voltar/atualizar a página sem perder a conversa.

   Proposta de ação (`tipo=PROPOSTA_ACAO`) renderiza um card dentro da
   bolha de mensagem (descrição, valor, categoria, prazo de expiração)
   com um botão "Confirmar" — que só **envia a mensagem "sim"**, não é
   um endpoint separado (a API do `ai-service` não tem um `/confirmar`
   dedicado de propósito — confirmação é conversacional, ver
   `ai-strategy.md`). Corrigir é simplesmente digitar outra coisa — sem
   botão dedicado, reflete a mesma simplificação já documentada na
   fatia 5 (correção = comando novo do zero, não merge incremental).

   Configuração de IA (`ConfiguracaoIaDialog`): provedor
   OpenAI (pede API key) ou Ollama (URL opcional). Banner no topo
   avisa se a IA ainda não foi configurada, sem bloquear a tela (deixa
   o usuário ver a configuração e mandar mensagem, que vai dar 422 com
   mensagem clara se tentar sem configurar — mesmo tratamento de erro
   já usado nos outros clients).

   `buscarConfiguracaoIa` envolvida em `cache()` do React — layout do
   chat e a página (nova conversa ou existente) chamam a mesma
   configuração na mesma renderização, dedupe evita duas chamadas HTTP
   idênticas (mesmo princípio já usado em `lib/auth-token.ts`).

   **Validado de ponta a ponta contra a stack real, incluindo o LLM de
   verdade** (não só o REST): configurei Ollama como provedor via API
   direta, mandei uma pergunta real
   ("quanto tenho disponivel pra gastar esse mes?") pro `ai-service` —
   primeira tentativa deu timeout (30s, modelo `llama3.1` ainda frio no
   Ollama), segunda tentativa (modelo já carregado em memória) voltou
   em 8s com "Você tem R$32508.67 disponível pra gastar em 2026-08" e
   `trace: [{"nome":"buscar_saldo_disponivel"}]` — o mesmo valor exato
   já confirmado no dashboard (item 6). Carreguei essa conversa real em
   `/chat/{id}` no navegador (via `curl` autenticado) e confirmei que o
   HTML renderizado contém a resposta de verdade — a primeira vez nesta
   fatia que dá pra validar um fluxo de IA de ponta a ponta pela UI, não
   só pela API. 5 testes automatizados novos (`ai-service.test.ts`).
10. ✅ Fechamento da fatia: CI (job `web` — `npm ci`/lint/test/`npm audit
   --omit=dev`/`npm run build`/build de imagem Docker de validação, sem
   OWASP Dependency-Check Maven aqui — ver `security.md` seção 6 e
   princípio 7 do `CLAUDE.md`), `docker-compose.yml` (serviço `web`,
   porta 3000, depende dos seis serviços), `.env.example` raiz
   (`AUTH_SECRET`), `diagrams.md` (container graph com `web` apontando
   pros seis serviços — `BudgetSvc` também estava faltando, corrigido
   junto), `overview.md` (tabela de clientes com porta/status),
   `roadmap.md` (fatia 6 → ✅ Entregue), `README.md` raiz.

   **Achado real rodando o `web` como container pela primeira vez**
   (nunca tinha sido testado fora de `npm run dev`): dois problemas reais
   de rede/sessão, só visíveis com o processo Next.js de fato isolado do
   navegador.
   - **Keycloak dentro do container**: o servidor Next.js (troca de
     código por token, renovação, userinfo, jwks) roda dentro do
     container `web`, que não alcança `localhost:8080` (é o próprio
     container — mesma classe de problema já resolvida nos serviços Java
     desde a fatia 1, mas o Auth.js exige solução diferente: não dá pra
     só trocar a URL, porque com discovery OIDC ligado o discovery
     document do Keycloak sempre devolve URLs "localhost", inalcançáveis
     de dentro do container). Corrigido desabilitando discovery no
     provider Keycloak (`wellKnown` omitido, `token`/`userinfo`/
     `jwks_endpoint` explícitos — ver `node_modules/@auth/core/src/lib/actions/callback/oauth/callback.ts`,
     que pula a chamada de discovery quando esses dois já vêm
     resolvidos) e separando `AUTH_KEYCLOAK_ISSUER` (público, só pro
     redirect de login/logout que o navegador precisa alcançar) de
     `AUTH_KEYCLOAK_INTERNAL_ISSUER` (novo, opcional — usado pelas
     chamadas de servidor: token/userinfo/jwks/refresh; em dev local sem
     Docker cai no mesmo valor do público). Validado de ponta a ponta
     via `curl` simulando o navegador (CSRF → redirect de login →
     formulário do Keycloak → callback) contra o container real: código
     trocado por token com sucesso, sessão criada.
   - **Cookie de sessão não encontrado dentro do container**: mesmo com
     o token trocado corretamente, `GET /` autenticado dava 500
     ("Sessão sem access token válido"). Causa raiz, achada por
     instrumentação temporária (`console.error` no `jwt()` callback e em
     `lib/auth-token.ts`, removida depois): a leitura manual do cookie
     (`getToken()` em `lib/auth-token.ts`, usada fora do fluxo do
     Auth.js pra propagar o access token aos microsserviços) decidia o
     NOME do cookie (`__Secure-authjs.session-token` vs
     `authjs.session-token` sem prefixo) usando `NODE_ENV`, mas o
     próprio Auth.js decide isso pelo protocolo REAL da requisição
     (http vs https). A imagem Docker tem `NODE_ENV=production` fixo
     (Dockerfile), mas em dev local via `docker-compose` o protocolo
     continua http (só produção real, atrás do Cloudflare Tunnel —
     ADR-0019 — é https de verdade) — `getToken()` procurava o nome
     errado e sempre devolvia `null`, apesar da sessão existir (`auth()`,
     que usa o protocolo real, decodificava o mesmo cookie sem problema
     — divergência só entre os dois mecanismos de leitura). Corrigido
     tentando os dois nomes de cookie em `obterTokenBruto()`, em vez de
     adivinhar por variável de ambiente — funciona em dev, atrás do
     Cloudflare Tunnel e em qualquer topologia futura, sem depender de
     header de proxy específico. Revalidado do zero (login completo via
     `curl` + `GET /`, `/contas`, `/transacoes`, `/cartoes`,
     `/documentos`, `/chat` autenticados, todos 200 contra o container
     real) — dashboard renderizou o nome do usuário e o "disponível pra
     gastar" reais.

   Kafka caiu no meio da sessão com `NodeExistsException` no Zookeeper
   (znode efêmero de sessão anterior, zumbi de infraestrutura sem
   relação com o `web`) — resolvido recriando `zookeeper`/`kafka`.

   `npm test` (52), `npm run lint`, `npm run build` e `npm audit
   --omit=dev` (0 vulnerabilidades) verdes; build da imagem Docker
   validado local e via `docker compose up -d --build` contra a stack
   real inteira (sete serviços de aplicação + toda a infra).
