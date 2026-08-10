package br.com.wepdev.financas.ai.infrastructure.llm;

import br.com.wepdev.financas.ai.domain.ConfiguracaoIa;
import br.com.wepdev.financas.ai.domain.LlmProvider;
import br.com.wepdev.financas.ai.domain.LlmProviderFactory;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class LlmProviderFactoryImpl implements LlmProviderFactory {

    private final OllamaRestClient ollamaRestClient;
    private final OpenAiRestClient openAiRestClient;
    private final String ollamaModeloChat;
    private final String ollamaModeloEmbedding;
    private final double ollamaTemperature;

    public LlmProviderFactoryImpl(@RestClient OllamaRestClient ollamaRestClient,
                                   @RestClient OpenAiRestClient openAiRestClient,
                                   @ConfigProperty(name = "ai-service.llm.ollama.model") String ollamaModeloChat,
                                   @ConfigProperty(name = "ai-service.llm.ollama.embedding-model") String ollamaModeloEmbedding,
                                   @ConfigProperty(name = "ai-service.llm.ollama.temperature", defaultValue = "0.1") double ollamaTemperature) {
        this.ollamaRestClient = ollamaRestClient;
        this.openAiRestClient = openAiRestClient;
        this.ollamaModeloChat = ollamaModeloChat;
        this.ollamaModeloEmbedding = ollamaModeloEmbedding;
        this.ollamaTemperature = ollamaTemperature;
    }

    @Override
    public LlmProvider criar(ConfiguracaoIa configuracao) {
        return switch (configuracao.getProvedor()) {
            case OPENAI -> new OpenAiLlmProvider(openAiRestClient, configuracao.getApiKey());
            case OLLAMA -> new OllamaLlmProvider(ollamaRestClient, ollamaModeloChat, ollamaModeloEmbedding, ollamaTemperature);
            case NENHUM -> new ProvedorNaoConfiguradoLlmProvider();
        };
    }

    @Override
    public LlmProvider criarParaEmbedding() {
        return new OllamaLlmProvider(ollamaRestClient, ollamaModeloChat, ollamaModeloEmbedding, ollamaTemperature);
    }
}
