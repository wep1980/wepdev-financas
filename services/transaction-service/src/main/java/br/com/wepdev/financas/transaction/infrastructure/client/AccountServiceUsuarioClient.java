package br.com.wepdev.financas.transaction.infrastructure.client;

import br.com.wepdev.financas.transaction.infrastructure.client.dto.ContaDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Chama GET /contas/{id} com o token do próprio usuário (role usuario) —
 * usado só pra confirmar que a conta existe e pertence a ele antes de
 * ajustar o saldo. Ver PropagarAutorizacaoHeadersFactory.
 */
@RegisterRestClient(configKey = "account-service")
@RegisterClientHeaders(PropagarAutorizacaoHeadersFactory.class)
public interface AccountServiceUsuarioClient {

    @GET
    @Path("/api/v1/contas/{id}")
    @Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    ContaDto buscarPorId(@PathParam("id") UUID id);
}
