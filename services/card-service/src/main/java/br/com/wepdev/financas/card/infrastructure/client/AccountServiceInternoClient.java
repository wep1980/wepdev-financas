package br.com.wepdev.financas.card.infrastructure.client;

import br.com.wepdev.financas.card.infrastructure.client.dto.AjusteSaldoRequestDto;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Chama o endpoint interno de débito (role service, ADR-0003), autenticado
 * via client_credentials do próprio client "card-service" no Keycloak —
 * token gerado e anexado automaticamente pelo quarkus-rest-client-oidc-filter
 * (ver application.properties, seção quarkus.oidc-client).
 */
@RegisterRestClient(configKey = "account-service")
@RegisterProvider(OidcClientRequestReactiveFilter.class)
public interface AccountServiceInternoClient {

    @POST
    @Path("/api/v1/contas/{id}/debitos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    void debitar(@PathParam("id") UUID id, AjusteSaldoRequestDto request);
}
