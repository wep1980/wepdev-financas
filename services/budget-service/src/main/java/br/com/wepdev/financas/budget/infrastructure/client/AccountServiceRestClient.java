package br.com.wepdev.financas.budget.infrastructure.client;

import br.com.wepdev.financas.budget.infrastructure.client.dto.ContaDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;
import java.util.List;

/** Chama GET /contas com o token do próprio usuário (role usuario) — ver PropagarAutorizacaoHeadersFactory. */
@RegisterRestClient(configKey = "account-service")
@RegisterClientHeaders(PropagarAutorizacaoHeadersFactory.class)
public interface AccountServiceRestClient {

    @GET
    @Path("/api/v1/contas")
    @Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    List<ContaDto> listarAtivas();
}
