package br.com.wepdev.financas.ai.infrastructure.client;

import br.com.wepdev.financas.ai.infrastructure.client.dto.DisponivelParaGastarDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;

/** Chama GET /disponivel-para-gastar com o token do próprio usuário (role usuario) — ver PropagarAutorizacaoHeadersFactory. */
@RegisterRestClient(configKey = "budget-service")
@RegisterClientHeaders(PropagarAutorizacaoHeadersFactory.class)
public interface BudgetServiceRestClient {

    @GET
    @Path("/api/v1/disponivel-para-gastar")
    @Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    DisponivelParaGastarDto disponivelParaGastar(@QueryParam("mes") String mes);
}
