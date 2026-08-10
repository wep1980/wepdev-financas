package br.com.wepdev.financas.budget.infrastructure.client;

import br.com.wepdev.financas.budget.infrastructure.client.dto.CartaoDto;
import br.com.wepdev.financas.budget.infrastructure.client.dto.FaturaDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/** Chama GET /cartoes e GET /cartoes/{id}/faturas com o token do próprio usuário (role usuario) — ver PropagarAutorizacaoHeadersFactory. */
@RegisterRestClient(configKey = "card-service")
@RegisterClientHeaders(PropagarAutorizacaoHeadersFactory.class)
public interface CardServiceRestClient {

    @GET
    @Path("/api/v1/cartoes")
    @Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    List<CartaoDto> listarCartoesAtivos();

    @GET
    @Path("/api/v1/cartoes/{id}/faturas")
    @Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    List<FaturaDto> listarFaturas(@PathParam("id") UUID cartaoId, @QueryParam("status") String status);
}
