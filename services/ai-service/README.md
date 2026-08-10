# ai-service

Chat em linguagem natural sobre a situação financeira do usuário (PRD
3.4) e execução de ações via comando de IA, sempre com confirmação
explícita (PRD 3.5, ADR-0007). Contrato: [`docs/specs/ai-service.yaml`](../../docs/specs/ai-service.yaml).

**Status: scaffold só** — projeto Quarkus criado (`pom.xml`, Dockerfiles,
`application.properties` porta 8086), ainda sem domínio/persistência/REST
implementados. Ver `docs/tasks.md` (fatia 5) pro backlog detalhado.

## LLM e RAG

Usa uma porta `LlmProvider` própria deste serviço (ADR-0002) — não
reaproveita a implementação do `document-service`; cada microsserviço
tem sua própria cópia de portas/adapters, sem lib compartilhada entre
serviços (ADR-0001). RAG sobre **Qdrant** (ADR-0005, confirmado
2026-08-10) pra busca semântica de descrição de transação/lançamento —
número exato (saldo, disponível pra gastar) sempre vem de query direta
via tool MCP, nunca de vetor (`ai-strategy.md` seção 2).

## Tools MCP

Tabela completa em [`docs/architecture/ai-strategy.md`](../../docs/architecture/ai-strategy.md)
seção 4. `criar_transacao` é a única tool de escrita do v1 (PRD 3.5) —
nunca executa sem confirmação explícita do usuário na mesma conversa
(ADR-0007).

## Armazenamento

MongoDB guarda o histórico de conversas (`Conversa`/`Mensagem`); Qdrant
guarda os embeddings pra busca semântica. Sem MySQL — este serviço não
tem dado relacional próprio, só consulta os outros serviços via tools
MCP.

## Rodando no IntelliJ

Mesma configuração dos outros serviços: Maven Home apontado pro `mvnw`
do projeto, variáveis de ambiente de dev (`KEYCLOAK_URL`,
`MONGO_ROOT_USER`/`MONGO_ROOT_PASSWORD`) já têm default no
`application.properties`, não precisa configurar nada extra pra rodar
`quarkus:dev`.
