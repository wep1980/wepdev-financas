package br.com.wepdev.financas.ai.application;

import br.com.wepdev.financas.ai.domain.EmbeddingResult;
import br.com.wepdev.financas.ai.domain.LlmProviderFactory;
import br.com.wepdev.financas.ai.domain.RegistroIndexado;
import br.com.wepdev.financas.ai.domain.VectorStore;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IndexarTransacaoUseCase {

    private final LlmProviderFactory llmProviderFactory;
    private final VectorStore vectorStore;

    public IndexarTransacaoUseCase(LlmProviderFactory llmProviderFactory, VectorStore vectorStore) {
        this.llmProviderFactory = llmProviderFactory;
        this.vectorStore = vectorStore;
    }

    /** Chamado pelo consumer de "transacao.eventos" — descrição de transação vira embedding (ai-strategy.md seção 2). */
    public void executar(IndexarTransacaoComando comando) {
        EmbeddingResult embedding = llmProviderFactory.criarParaEmbedding().embed(comando.descricao());
        vectorStore.indexar(new RegistroIndexado(comando.transacaoId(), comando.usuarioId(), comando.descricao(),
                embedding.vetor()));
    }
}
