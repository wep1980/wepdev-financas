# ADR-0005: Qdrant como vector store para RAG

Status: Aceita (confirmada pelo usuário em 2026-08-10, início da fatia 5)
Data: 2026-08-06

## Contexto

RAG (seção 2-3 de `docs/architecture/ai-strategy.md`) precisa de um vector
store para embeddings de transações/lançamentos. A stack já tem MongoDB, mas
MongoDB self-hosted (não Atlas) não tem busca vetorial nativa madura — Atlas
Vector Search exigiria migrar pra MongoDB gerenciado na nuvem, o que
contradiz a decisão de manter tudo self-hosted via Docker Compose em dev.

## Decisão (proposta)

Adicionar Qdrant como serviço no `docker-compose.yml`, self-hosted, dedicado
a embeddings. Alternativas consideradas:

- **pgvector**: exigiria introduzir Postgres na stack só pra isso (stack
  atual usa MySQL para dado relacional) — rejeitado por adicionar um SGBD
  novo sem necessidade.
- **MongoDB Atlas Vector Search**: exigiria conta cloud gerenciada,
  contradiz "tudo sobe local via Docker Compose em dev" — rejeitado por ora.
- **Weaviate/Milvus**: alternativas válidas, mais pesadas operacionalmente
  que Qdrant para o volume de dado esperado (uso pessoal/poucos usuários
  inicialmente) — Qdrant escolhido por simplicidade de operação.

## Consequências

- Mais um serviço no `docker-compose.yml` (mais um container em dev).
- Precisa de um cliente Qdrant no `ai-service` (Java) — verificar maturidade
  do client oficial/comunidade pra Java/Quarkus antes de implementar.
