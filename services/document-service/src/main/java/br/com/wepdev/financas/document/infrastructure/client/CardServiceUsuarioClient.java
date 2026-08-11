package br.com.wepdev.financas.document.infrastructure.client;

import br.com.wepdev.financas.document.infrastructure.client.dto.CompraResumoDto;
import br.com.wepdev.financas.document.infrastructure.client.dto.LancarCompraRequestDto;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Chama /api/v1/cartoes com o token do próprio usuário (role usuario) —
 * o card-service já rejeita com 404 se o cartão não pertencer a ele
 * (mesmo padrão de {@code AccountServiceUsuarioClient} antes de ser
 * removido, ADR-0028). Ver PropagarAutorizacaoHeadersFactory.
 */
@RegisterRestClient(configKey = "card-service")
@RegisterClientHeaders(PropagarAutorizacaoHeadersFactory.class)
public interface CardServiceUsuarioClient {

    @GET
    @Path("/api/v1/cartoes/{id}/compras")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    List<CompraResumoDto> listarCompras(@PathParam("id") UUID cartaoId);

    /**
     * Sem {@code @Retry} de propósito: POST não é idempotente aqui (sem
     * idempotency key no card-service) — reenviar depois de um timeout
     * poderia duplicar a compra se a primeira tentativa tiver de fato
     * chegado no servidor. Reexecutar a confirmação inteira já é seguro
     * (dedup em ConfirmarLancamentosUseCase), então uma falha aqui só
     * precisa propagar, não tentar de novo sozinha.
     */
    @POST
    @Path("/api/v1/cartoes/{id}/compras")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    void lancarCompra(@PathParam("id") UUID cartaoId, LancarCompraRequestDto request);
}
