package br.com.wepdev.financas.ai.infrastructure.llm;

import br.com.wepdev.financas.ai.domain.ChatRequest;
import br.com.wepdev.financas.ai.domain.ChatResponse;
import br.com.wepdev.financas.ai.domain.EmbeddingResult;
import br.com.wepdev.financas.ai.domain.IaNaoConfiguradaException;
import br.com.wepdev.financas.ai.domain.LlmProvider;

/** Null object pra ProvedorIa.NENHUM — nunca chama nada de verdade, só sinaliza o erro de negócio (ADR-0002). */
public class ProvedorNaoConfiguradoLlmProvider implements LlmProvider {

    @Override
    public ChatResponse chat(ChatRequest request) {
        throw new IaNaoConfiguradaException();
    }

    @Override
    public EmbeddingResult embed(String texto) {
        throw new IaNaoConfiguradaException();
    }

    @Override
    public boolean isConfigured() {
        return false;
    }
}
