package br.com.wepdev.financas.document.infrastructure.llm;

import br.com.wepdev.financas.document.infrastructure.llm.dto.OllamaGenerateRequestDto;
import br.com.wepdev.financas.document.infrastructure.llm.dto.OllamaGenerateResponseDto;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;

@RegisterRestClient(configKey = "ollama")
public interface OllamaRestClient {

    /**
     * Timeout generoso — inferência local em CPU é lenta (chamada de teste
     * real levou ~34s pro llama3.1 carregar + responder, ver
     * docs/historico.md 2026-08-09). Modelo fica carregado em memória entre
     * chamadas, então só a primeira costuma ser tão lenta assim.
     */
    @POST
    @Path("/api/generate")
    @Timeout(value = 120, unit = ChronoUnit.SECONDS)
    OllamaGenerateResponseDto gerar(OllamaGenerateRequestDto request);
}
