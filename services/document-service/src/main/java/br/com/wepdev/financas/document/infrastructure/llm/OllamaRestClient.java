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
     * Timeout generoso — inferência local em CPU é lenta. Testado na
     * prática em 2026-08-10 com uma fatura real (test-data/Nubank_2026-07-17.pdf,
     * llama3.1 8B em CPU): nem 120s nem 300s foram suficientes — extração
     * de fatura completa (prompt grande) é bem mais lenta que uma pergunta
     * curta de chat (~8s, ver docs/historico.md fatia 6 item 9). Subido
     * pra 600s (10min) como margem de segurança; se ainda não bastar, o
     * gargalo real é a velocidade de inferência em CPU, não o timeout —
     * nesse caso vale considerar modelo menor/quantizado, ou GPU, antes de
     * simplesmente subir o número de novo.
     */
    @POST
    @Path("/api/generate")
    @Timeout(value = 600, unit = ChronoUnit.SECONDS)
    OllamaGenerateResponseDto gerar(OllamaGenerateRequestDto request);
}
