package br.com.wepdev.financas.ai.infrastructure.llm;

import br.com.wepdev.financas.ai.domain.ConfiguracaoIa;
import br.com.wepdev.financas.ai.domain.LlmProvider;
import br.com.wepdev.financas.ai.domain.ProvedorIa;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LlmProviderFactoryImplTest {

    private final OllamaRestClient ollamaRestClient = mock(OllamaRestClient.class);
    private final OpenAiRestClient openAiRestClient = mock(OpenAiRestClient.class);
    private final LlmProviderFactoryImpl factory = new LlmProviderFactoryImpl(
            ollamaRestClient, openAiRestClient, "llama3.1:latest", "nomic-embed-text", 0.1);

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void deveriaCriarOllamaLlmProvider_quandoProvedorOllama() {
        ConfiguracaoIa configuracao = ConfiguracaoIa.definir(usuarioId, ProvedorIa.OLLAMA, null, null);

        LlmProvider provider = factory.criar(configuracao);

        assertThat(provider).isInstanceOf(OllamaLlmProvider.class);
        assertThat(provider.isConfigured()).isTrue();
    }

    @Test
    void deveriaCriarOpenAiLlmProvider_comApiKeyDoUsuario() {
        ConfiguracaoIa configuracao = ConfiguracaoIa.definir(usuarioId, ProvedorIa.OPENAI, "sk-teste", null);

        LlmProvider provider = factory.criar(configuracao);

        assertThat(provider).isInstanceOf(OpenAiLlmProvider.class);
        assertThat(provider.isConfigured()).isTrue();
    }

    @Test
    void deveriaCriarProviderNaoConfigurado_quandoProvedorNenhum() {
        ConfiguracaoIa configuracao = ConfiguracaoIa.semDefinir(usuarioId);

        LlmProvider provider = factory.criar(configuracao);

        assertThat(provider).isInstanceOf(ProvedorNaoConfiguradoLlmProvider.class);
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    void criarParaEmbedding_deveriaSempreDevolverOllama_independenteDoProvedorDeChatDoUsuario() {
        LlmProvider provider = factory.criarParaEmbedding();

        assertThat(provider).isInstanceOf(OllamaLlmProvider.class);
        assertThat(provider.isConfigured()).isTrue();
    }
}
