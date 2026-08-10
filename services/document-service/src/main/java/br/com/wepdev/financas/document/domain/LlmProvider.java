package br.com.wepdev.financas.document.domain;

/**
 * Porta de saída pro LLM (ADR-0002) — implementada localmente aqui, não em
 * {@code ai-service} (que ainda não existe, ver
 * {@code docs/architecture/ai-strategy.md} seção 4: "o agente de parsing de
 * documento vive logicamente perto do document-service mas usa o mesmo
 * LlmProvider"). Só {@code chat()} por enquanto — é só o que o agente de
 * extração de fatura (item 4) precisa. {@code embed()} (RAG) e suporte a
 * imagem (ADR-0015, ingestão por foto) entram quando essas fatias forem
 * implementadas, não adiantar agora.
 */
public interface LlmProvider {

    ChatResponse chat(ChatRequest request);

    /** Falso = funcionalidade de IA indisponível, erro de negócio claro pro usuário (ai-strategy.md seção 1). */
    boolean isConfigured();
}
