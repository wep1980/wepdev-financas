package br.com.wepdev.financas.ai.infrastructure.llm;

import br.com.wepdev.financas.ai.infrastructure.llm.dto.OpenAiChatRequestDto;
import br.com.wepdev.financas.ai.infrastructure.llm.dto.OpenAiChatResponseDto;
import br.com.wepdev.financas.ai.infrastructure.llm.dto.OpenAiEmbeddingRequestDto;
import br.com.wepdev.financas.ai.infrastructure.llm.dto.OpenAiEmbeddingResponseDto;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;

/**
 * Base URL sempre fixa ({@code https://api.openai.com}) — só a API key
 * varia por usuário (ADR-0002), por isso passada como header por
 * chamada em vez de configuração de client fixa (nunca dá pra saber
 * qual usuário antes da chamada acontecer).
 */
@RegisterRestClient(configKey = "openai")
public interface OpenAiRestClient {

    @POST
    @Path("/v1/chat/completions")
    @Timeout(value = 60, unit = ChronoUnit.SECONDS)
    OpenAiChatResponseDto criarChatCompletion(@HeaderParam("Authorization") String autorizacao, OpenAiChatRequestDto request);

    @POST
    @Path("/v1/embeddings")
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    OpenAiEmbeddingResponseDto criarEmbedding(@HeaderParam("Authorization") String autorizacao, OpenAiEmbeddingRequestDto request);
}
