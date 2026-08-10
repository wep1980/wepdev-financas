package br.com.wepdev.financas.ai.infrastructure.llm;

import br.com.wepdev.financas.ai.infrastructure.llm.dto.OllamaEmbeddingsRequestDto;
import br.com.wepdev.financas.ai.infrastructure.llm.dto.OllamaEmbeddingsResponseDto;
import br.com.wepdev.financas.ai.infrastructure.llm.dto.OllamaGenerateRequestDto;
import br.com.wepdev.financas.ai.infrastructure.llm.dto.OllamaGenerateResponseDto;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;

@RegisterRestClient(configKey = "ollama")
public interface OllamaRestClient {

    /** Timeout generoso — inferência local em CPU é lenta (mesmo achado do document-service, docs/historico.md 2026-08-09). */
    @POST
    @Path("/api/generate")
    @Timeout(value = 120, unit = ChronoUnit.SECONDS)
    OllamaGenerateResponseDto gerar(OllamaGenerateRequestDto request);

    @POST
    @Path("/api/embeddings")
    @Timeout(value = 60, unit = ChronoUnit.SECONDS)
    OllamaEmbeddingsResponseDto embeddings(OllamaEmbeddingsRequestDto request);
}
