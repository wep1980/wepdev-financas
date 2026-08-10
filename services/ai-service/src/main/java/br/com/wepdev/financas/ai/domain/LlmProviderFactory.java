package br.com.wepdev.financas.ai.domain;

/**
 * Resolve qual {@link LlmProvider} usar a partir da configuração do
 * usuário (ADR-0002 — "cada usuário escolhe o seu"). Existe pra manter
 * {@link LlmProvider} sem nenhum parâmetro de credencial/config — quem
 * decide isso é a factory, uma vez por chamada, não a porta em si.
 */
public interface LlmProviderFactory {

    /** Nunca falha — se ConfiguracaoIa.provedor for NENHUM, devolve um provider com isConfigured()=false. */
    LlmProvider criar(ConfiguracaoIa configuracao);

    /**
     * RAG (item 7) sempre usa Ollama local pra embedding, **independente**
     * do provedor de chat escolhido pelo usuário — modelos diferentes
     * geram vetores de dimensão diferente (ex: nomic-embed-text = 768,
     * text-embedding-3-small da OpenAI = 1536), e o Qdrant precisa de uma
     * dimensão fixa por coleção. Misturar provedor de embedding por
     * usuário quebraria a coleção. Indexação/busca não dependem de
     * ConfiguracaoIa nenhuma por causa disso.
     */
    LlmProvider criarParaEmbedding();
}
