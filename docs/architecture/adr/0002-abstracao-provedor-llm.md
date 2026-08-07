# ADR-0002: Abstração de provedor de LLM (OpenAI/Ollama)

Status: Aceita
Data: 2026-08-06

## Contexto

O sistema precisa responder perguntas em linguagem natural sobre dados
financeiros sensíveis. Dois provedores fazem sentido por razões opostas:
OpenAI (via API key do próprio usuário) por qualidade/maturidade de API, e
Ollama (local) por privacidade total e custo zero por uso. Nenhum dos dois
deveria ser uma dependência rígida do design do `ai-service`.

## Decisão

Definir uma porta `LlmProvider` (interface) com métodos `chat`, `embed` e
`isConfigured`, e dois adapters (`OpenAiLlmProvider`, `OllamaLlmProvider`)
implementando essa porta. O provedor é escolhido por configuração do usuário,
resolvido em runtime. Nenhuma classe de domínio ou de orquestração de agente
depende de SDK de provedor específico — só da porta. Detalhe técnico em
`docs/architecture/ai-strategy.md`.

## Consequências

- Mais trabalho de design inicial (definir bem a porta antes de implementar
  qualquer adapter) do que simplesmente chamar a API da OpenAI direto.
- Zero retrabalho pra adicionar um terceiro provedor no futuro (ex: Anthropic,
  Azure OpenAI) — só mais um adapter.
- A porta precisa ser desenhada em torno do que os dois provedores têm em
  comum (chat + embeddings); funcionalidades exclusivas de um provedor
  específico não entram na porta — se viram necessárias, tratar como
  capability opcional (`isConfigured`-like check), não quebrar a
  abstração.
- Testes de agente/orquestração usam mock da porta (ver
  `docs/architecture/testing-strategy.md`), nunca sobem um provedor real.
