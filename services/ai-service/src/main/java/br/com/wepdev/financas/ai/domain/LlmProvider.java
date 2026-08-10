package br.com.wepdev.financas.ai.domain;

/**
 * Porta de saída pro LLM (ADR-0002, ai-strategy.md seção 1) — cópia
 * própria deste serviço, mesma forma da porta já usada no
 * document-service, com {@code embed()} a mais (precisa de RAG aqui,
 * document-service não precisava). Cada chamada já é feita numa
 * instância resolvida pro provedor/credencial certos — ver
 * {@link LlmProviderFactory}, que decide qual adapter instanciar a
 * partir da {@link ConfiguracaoIa} do usuário. Isso mantém esta porta
 * sem nenhum parâmetro de configuração, exatamente como documentado.
 */
public interface LlmProvider {

    ChatResponse chat(ChatRequest request);

    EmbeddingResult embed(String texto);

    /** Falso = funcionalidade de IA indisponível, erro de negócio claro pro usuário (ai-strategy.md seção 1). */
    boolean isConfigured();
}
