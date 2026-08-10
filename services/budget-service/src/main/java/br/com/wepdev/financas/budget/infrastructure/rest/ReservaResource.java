package br.com.wepdev.financas.budget.infrastructure.rest;

import br.com.wepdev.financas.budget.application.BuscarReservaUseCase;
import br.com.wepdev.financas.budget.application.DefinirReservaCommand;
import br.com.wepdev.financas.budget.application.DefinirReservaUseCase;
import br.com.wepdev.financas.budget.domain.Reserva;
import br.com.wepdev.financas.budget.infrastructure.rest.dto.DefinirReservaRequest;
import br.com.wepdev.financas.budget.infrastructure.rest.dto.ReservaResponse;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

/** Conforme docs/specs/budget-service.yaml — spec-driven, contrato antes do código. */
@Path("/api/v1/reserva")
public class ReservaResource {

    private final BuscarReservaUseCase buscarReservaUseCase;
    private final DefinirReservaUseCase definirReservaUseCase;
    private final SecurityIdentity identity;

    public ReservaResource(BuscarReservaUseCase buscarReservaUseCase, DefinirReservaUseCase definirReservaUseCase,
                            SecurityIdentity identity) {
        this.buscarReservaUseCase = buscarReservaUseCase;
        this.definirReservaUseCase = definirReservaUseCase;
        this.identity = identity;
    }

    @GET
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public ReservaResponse buscar() {
        return ReservaResponse.de(buscarReservaUseCase.executar(usuarioIdAutenticado()));
    }

    @PUT
    @RolesAllowed("usuario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ReservaResponse definir(@Valid DefinirReservaRequest request) {
        Reserva reserva = definirReservaUseCase.executar(new DefinirReservaCommand(usuarioIdAutenticado(), request.valor()));
        return ReservaResponse.de(reserva);
    }

    /** sub do token OIDC = id do usuário no Keycloak — nunca aceitar usuarioId vindo do cliente (ADR-0003). */
    private UUID usuarioIdAutenticado() {
        if (identity.getPrincipal() instanceof JsonWebToken jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        throw new IllegalStateException("Token autenticado não é um JWT com claim 'sub'");
    }
}
