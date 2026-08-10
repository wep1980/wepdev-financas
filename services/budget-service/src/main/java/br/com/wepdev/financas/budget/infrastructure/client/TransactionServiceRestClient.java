package br.com.wepdev.financas.budget.infrastructure.client;

import br.com.wepdev.financas.budget.infrastructure.client.dto.ResumoCategoriaDto;
import br.com.wepdev.financas.budget.infrastructure.client.dto.TransacaoRecorrenteDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Chama GET /transacoes-recorrentes e GET /transacoes/resumo-por-categoria
 * com o token do próprio usuário (role usuario) — ver
 * PropagarAutorizacaoHeadersFactory.
 */
@RegisterRestClient(configKey = "transaction-service")
@RegisterClientHeaders(PropagarAutorizacaoHeadersFactory.class)
public interface TransactionServiceRestClient {

    @GET
    @Path("/api/v1/transacoes-recorrentes")
    @Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    List<TransacaoRecorrenteDto> listarRecorrentes(@QueryParam("status") String status);

    @GET
    @Path("/api/v1/transacoes/resumo-por-categoria")
    @Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    List<ResumoCategoriaDto> resumoPorCategoria(@QueryParam("inicio") LocalDate inicio, @QueryParam("fim") LocalDate fim);
}
