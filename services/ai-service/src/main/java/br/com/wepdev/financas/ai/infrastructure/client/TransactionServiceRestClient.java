package br.com.wepdev.financas.ai.infrastructure.client;

import br.com.wepdev.financas.ai.infrastructure.client.dto.CriarTransacaoRecorrenteRequestDto;
import br.com.wepdev.financas.ai.infrastructure.client.dto.CriarTransacaoRequestDto;
import br.com.wepdev.financas.ai.infrastructure.client.dto.ResumoCategoriaDto;
import br.com.wepdev.financas.ai.infrastructure.client.dto.TransacaoDto;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Chama /transacoes, /transacoes/resumo-por-categoria e
 * /transacoes-recorrentes com o token do próprio usuário (role usuario)
 * — ver PropagarAutorizacaoHeadersFactory. criarTransacao/criarRecorrente
 * só são chamados depois de confirmação explícita do usuário (ADR-0007,
 * decidido no agente orquestrador, item 8 — não aqui).
 */
@RegisterRestClient(configKey = "transaction-service")
@RegisterClientHeaders(PropagarAutorizacaoHeadersFactory.class)
public interface TransactionServiceRestClient {

    @GET
    @Path("/api/v1/transacoes")
    @Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    List<TransacaoDto> listar(@QueryParam("inicio") LocalDate inicio, @QueryParam("fim") LocalDate fim);

    @GET
    @Path("/api/v1/transacoes/resumo-por-categoria")
    @Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    List<ResumoCategoriaDto> resumoPorCategoria(@QueryParam("inicio") LocalDate inicio, @QueryParam("fim") LocalDate fim);

    @POST
    @Path("/api/v1/transacoes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    TransacaoDto criar(CriarTransacaoRequestDto request);

    @POST
    @Path("/api/v1/transacoes-recorrentes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    void criarRecorrente(CriarTransacaoRecorrenteRequestDto request);
}
