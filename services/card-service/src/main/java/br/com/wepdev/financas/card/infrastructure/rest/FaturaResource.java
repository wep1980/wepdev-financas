package br.com.wepdev.financas.card.infrastructure.rest;

import br.com.wepdev.financas.card.application.BuscarFaturaUseCase;
import br.com.wepdev.financas.card.application.PagarFaturaUseCase;
import br.com.wepdev.financas.card.application.ProximosVencimentosUseCase;
import br.com.wepdev.financas.card.infrastructure.rest.dto.FaturaDetalheResponse;
import br.com.wepdev.financas.card.infrastructure.rest.dto.ProximoVencimentoFaturaResponse;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Conforme docs/specs/card-service.yaml — spec-driven, contrato antes do código. */
@Path("/api/v1/faturas")
public class FaturaResource {

    private final BuscarFaturaUseCase buscarFaturaUseCase;
    private final PagarFaturaUseCase pagarFaturaUseCase;
    private final ProximosVencimentosUseCase proximosVencimentosUseCase;
    private final SecurityIdentity identity;

    public FaturaResource(BuscarFaturaUseCase buscarFaturaUseCase, PagarFaturaUseCase pagarFaturaUseCase,
                           ProximosVencimentosUseCase proximosVencimentosUseCase, SecurityIdentity identity) {
        this.buscarFaturaUseCase = buscarFaturaUseCase;
        this.pagarFaturaUseCase = pagarFaturaUseCase;
        this.proximosVencimentosUseCase = proximosVencimentosUseCase;
        this.identity = identity;
    }

    @GET
    @Path("/proximos-vencimentos")
    @RolesAllowed("service")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProximoVencimentoFaturaResponse> proximosVencimentos(@NotNull @QueryParam("dias") Integer dias) {
        return proximosVencimentosUseCase.executar(LocalDate.now(), dias).stream()
                .map(ProximoVencimentoFaturaResponse::de)
                .toList();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public FaturaDetalheResponse buscarPorId(@PathParam("id") UUID id) {
        return FaturaDetalheResponse.de(buscarFaturaUseCase.executar(id, usuarioIdAutenticado()));
    }

    @POST
    @Path("/{id}/pagar")
    @RolesAllowed("usuario")
    public Response pagar(@PathParam("id") UUID id) {
        pagarFaturaUseCase.executar(id, usuarioIdAutenticado());
        return Response.noContent().build();
    }

    /** sub do token OIDC = id do usuário no Keycloak — nunca aceitar usuarioId vindo do cliente (ADR-0003). */
    private UUID usuarioIdAutenticado() {
        if (identity.getPrincipal() instanceof JsonWebToken jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        throw new IllegalStateException("Token autenticado não é um JWT com claim 'sub'");
    }
}
