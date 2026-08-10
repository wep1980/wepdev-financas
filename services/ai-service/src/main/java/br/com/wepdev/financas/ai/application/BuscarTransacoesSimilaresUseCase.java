package br.com.wepdev.financas.ai.application;

import br.com.wepdev.financas.ai.domain.EmbeddingResult;
import br.com.wepdev.financas.ai.domain.LlmProviderFactory;
import br.com.wepdev.financas.ai.domain.ResultadoBusca;
import br.com.wepdev.financas.ai.domain.VectorStore;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/** Parte semântica da tool MCP buscar_transacoes (ai-strategy.md seção 4) — o agente (item 8) combina isso com o filtro relacional do transaction-service. */
@ApplicationScoped
public class BuscarTransacoesSimilaresUseCase {

    private final LlmProviderFactory llmProviderFactory;
    private final VectorStore vectorStore;

    public BuscarTransacoesSimilaresUseCase(LlmProviderFactory llmProviderFactory, VectorStore vectorStore) {
        this.llmProviderFactory = llmProviderFactory;
        this.vectorStore = vectorStore;
    }

    public List<ResultadoBusca> executar(UUID usuarioId, String consultaEmTextoLivre, int limite) {
        EmbeddingResult embedding = llmProviderFactory.criarParaEmbedding().embed(consultaEmTextoLivre);
        return vectorStore.buscarSimilares(usuarioId, embedding.vetor(), limite);
    }
}
