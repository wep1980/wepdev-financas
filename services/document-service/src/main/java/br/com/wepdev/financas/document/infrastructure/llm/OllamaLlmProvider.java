package br.com.wepdev.financas.document.infrastructure.llm;

import br.com.wepdev.financas.document.domain.ChatRequest;
import br.com.wepdev.financas.document.domain.ChatResponse;
import br.com.wepdev.financas.document.domain.LlmProvider;
import br.com.wepdev.financas.document.infrastructure.llm.dto.OllamaGenerateRequestDto;
import br.com.wepdev.financas.document.infrastructure.llm.dto.OllamaOptionsDto;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Único provedor implementado nessa fatia (ADR-0002/ai-strategy.md) — chama
 * uma instância Ollama local, sem custo por chamada, dado financeiro
 * sensível não sai da máquina.
 */
@ApplicationScoped
public class OllamaLlmProvider implements LlmProvider {

    @RestClient
    OllamaRestClient restClient;

    @ConfigProperty(name = "document-service.llm.ollama.model")
    String model;

    @ConfigProperty(name = "document-service.llm.ollama.temperature", defaultValue = "0.1")
    double temperature;

    @Override
    public ChatResponse chat(ChatRequest request) {
        String formato = request.formatoJson() ? "json" : null;
        var resposta = restClient.gerar(new OllamaGenerateRequestDto(model, request.prompt(), false, formato,
                new OllamaOptionsDto(temperature)));
        return new ChatResponse(resposta.response());
    }

    /** Ollama local não tem chave de usuário pra faltar (ao contrário de um futuro OpenAiLlmProvider) — sempre configurado. */
    @Override
    public boolean isConfigured() {
        return true;
    }
}
