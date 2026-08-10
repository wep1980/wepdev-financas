package br.com.wepdev.financas.ai.infrastructure.llm;

import br.com.wepdev.financas.ai.domain.ChatRequest;
import br.com.wepdev.financas.ai.domain.ChatResponse;
import br.com.wepdev.financas.ai.domain.EmbeddingResult;
import br.com.wepdev.financas.ai.domain.LlmProvider;
import br.com.wepdev.financas.ai.infrastructure.llm.dto.OllamaEmbeddingsRequestDto;
import br.com.wepdev.financas.ai.infrastructure.llm.dto.OllamaGenerateRequestDto;
import br.com.wepdev.financas.ai.infrastructure.llm.dto.OllamaOptionsDto;

/**
 * Instanciado pela {@link br.com.wepdev.financas.ai.domain.LlmProviderFactory}
 * a cada chamada — não é bean CDI (diferente do resto do projeto), porque
 * carrega o modelo/temperatura resolvidos, não tem estado de credencial
 * por usuário como o {@link OpenAiLlmProvider} (Ollama local não tem
 * chave que falte). {@code ConfiguracaoIa.ollamaUrl} (URL customizada
 * por usuário) ainda **não** é usado aqui — sempre usa a instância
 * default configurada em application.properties; suportar URL
 * customizada por usuário fica pra quando isso virar necessidade real
 * (ver docs/tasks.md item 5).
 */
public class OllamaLlmProvider implements LlmProvider {

    private final OllamaRestClient restClient;
    private final String modeloChat;
    private final String modeloEmbedding;
    private final double temperature;

    public OllamaLlmProvider(OllamaRestClient restClient, String modeloChat, String modeloEmbedding, double temperature) {
        this.restClient = restClient;
        this.modeloChat = modeloChat;
        this.modeloEmbedding = modeloEmbedding;
        this.temperature = temperature;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String formato = request.formatoJson() ? "json" : null;
        var resposta = restClient.gerar(new OllamaGenerateRequestDto(modeloChat, request.prompt(), false, formato,
                new OllamaOptionsDto(temperature)));
        return new ChatResponse(resposta.response());
    }

    @Override
    public EmbeddingResult embed(String texto) {
        var resposta = restClient.embeddings(new OllamaEmbeddingsRequestDto(modeloEmbedding, texto));
        return new EmbeddingResult(resposta.embedding());
    }

    /** Ollama local não tem chave de usuário pra faltar (ao contrário do OpenAiLlmProvider) — sempre configurado. */
    @Override
    public boolean isConfigured() {
        return true;
    }
}
