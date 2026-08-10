# document-service

Ingestão de documentos financeiros (fatura de cartão, extrato bancário,
boleto — ADR-0004) via upload, extração de lançamentos candidatos e fluxo
de confirmação obrigatória antes de gerar transação de verdade (PRD 3.2).
Contrato: [`docs/specs/document-service.yaml`](../../docs/specs/document-service.yaml).

**Status: scaffold só** — projeto Quarkus criado (`pom.xml`, Dockerfiles,
`application.properties` porta 8084), ainda sem domínio/persistência/REST
implementados. Ver `docs/tasks.md` (fatia 3) pro backlog detalhado.

## Escopo da primeira fatia

Só fatura de cartão em PDF. Decisões de arquitetura registradas em
[ADR-0023](../../docs/architecture/adr/0023-document-service-primeira-fatia-escopo.md):
essa fatia **não integra** com o modelo `Fatura`/`Parcela` do
`card-service` — cada lançamento confirmado vira uma `Transacao` avulsa
comum, publicada via evento Kafka `documento.lancamentos-confirmados` e
consumida pelo `transaction-service` (fluxo completo em
`docs/architecture/overview.md` seção 3). Extração de texto do PDF via
Apache PDFBox.

## LLM

Usa a porta `LlmProvider` (ADR-0002), implementada localmente nesse
serviço (não em `ai-service`, que ainda não existe — ver
`docs/architecture/ai-strategy.md` seção 4). Provedor de dev/teste é
**Ollama local** (`docker-compose.yml`, serviço `ollama`, modelo
`llama3.1` já baixado — `OLLAMA_BASE_URL` default
`http://localhost:11434`). Dado financeiro sensível não sai da máquina
nesse modo.

## Armazenamento

MongoDB guarda o documento bruto (PDF) + resultado do parsing
(`DocumentoImportado`); MySQL guarda `LancamentoPendente`, queryable e
referenciado pelo id do documento no Mongo — mesmo split descrito na
tabela de serviços de `overview.md`.

## Rodando no IntelliJ

Mesma configuração dos outros serviços (`account-service`/`card-service`):
Maven Home apontado pro `mvnw` do projeto, variáveis de ambiente de dev
(`DB_USER`, `DB_PASSWORD`, `KEYCLOAK_URL`) já têm default no
`application.properties`, não precisa configurar nada extra pra rodar
`quarkus:dev`.
