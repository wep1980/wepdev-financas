package br.com.wepdev.financas.ai.infrastructure.llm;

import br.com.wepdev.financas.ai.domain.ChatRequest;
import br.com.wepdev.financas.ai.domain.ChatResponse;
import br.com.wepdev.financas.ai.domain.EmbeddingResult;
import br.com.wepdev.financas.ai.domain.LlmProvider;
import br.com.wepdev.financas.ai.infrastructure.llm.dto.OpenAiChatRequestDto;
import br.com.wepdev.financas.ai.infrastructure.llm.dto.OpenAiEmbeddingRequestDto;
import br.com.wepdev.financas.ai.infrastructure.llm.dto.OpenAiMensagemDto;
import br.com.wepdev.financas.ai.infrastructure.llm.dto.OpenAiResponseFormatDto;

import java.util.List;

/**
 * Instanciado pela {@link br.com.wepdev.financas.ai.domain.LlmProviderFactory}
 * a cada chamada, carregando a {@code apiKey} do usuário resolvido — não
 * é bean CDI porque a credencial muda por chamada (o REST client em si
 * ({@code restClient}) continua sendo o singleton injetado via CDI, só a
 * base URL da OpenAI é fixa, então isso não precisa de client dinâmico).
 */
public class OpenAiLlmProvider implements LlmProvider {

    private static final String MODELO_CHAT = "gpt-4o-mini";
    private static final String MODELO_EMBEDDING = "text-embedding-3-small";

    private final OpenAiRestClient restClient;
    private final String apiKey;

    public OpenAiLlmProvider(OpenAiRestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        OpenAiResponseFormatDto formato = request.formatoJson() ? OpenAiResponseFormatDto.json() : null;
        var resposta = restClient.criarChatCompletion(autorizacao(),
                new OpenAiChatRequestDto(MODELO_CHAT, List.of(new OpenAiMensagemDto("user", request.prompt())), formato));
        return new ChatResponse(resposta.choices().get(0).message().content());
    }

    @Override
    public EmbeddingResult embed(String texto) {
        var resposta = restClient.criarEmbedding(autorizacao(), new OpenAiEmbeddingRequestDto(MODELO_EMBEDDING, texto));
        return new EmbeddingResult(resposta.data().get(0).embedding());
    }

    /** apiKey nunca logada — se estiver ausente/vazia, isConfigured() já teria barrado a chamada antes de chegar aqui. */
    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private String autorizacao() {
        return "Bearer " + apiKey;
    }
}
