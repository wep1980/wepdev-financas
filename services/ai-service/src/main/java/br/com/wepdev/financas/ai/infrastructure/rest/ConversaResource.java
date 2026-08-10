package br.com.wepdev.financas.ai.infrastructure.rest;

import br.com.wepdev.financas.ai.application.BuscarConversaUseCase;
import br.com.wepdev.financas.ai.application.ListarConversasUseCase;
import br.com.wepdev.financas.ai.infrastructure.rest.dto.ConversaDetalheResponse;
import br.com.wepdev.financas.ai.infrastructure.rest.dto.ConversaResumo;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

/** Conforme docs/specs/ai-service.yaml — spec-driven, contrato antes do código. */
@Path("/api/v1/conversas")
public class ConversaResource {

    private final ListarConversasUseCase listarConversasUseCase;
    private final BuscarConversaUseCase buscarConversaUseCase;
    private final SecurityIdentity identity;

    public ConversaResource(ListarConversasUseCase listarConversasUseCase, BuscarConversaUseCase buscarConversaUseCase,
                             SecurityIdentity identity) {
        this.listarConversasUseCase = listarConversasUseCase;
        this.buscarConversaUseCase = buscarConversaUseCase;
        this.identity = identity;
    }

    @GET
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ConversaResumo> listar() {
        return listarConversasUseCase.executar(usuarioIdAutenticado()).stream()
                .map(ConversaResumo::de)
                .toList();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public ConversaDetalheResponse buscarPorId(@PathParam("id") UUID id) {
        return ConversaDetalheResponse.de(buscarConversaUseCase.executar(id, usuarioIdAutenticado()));
    }

    /** sub do token OIDC = id do usuário no Keycloak — nunca aceitar usuarioId vindo do cliente (ADR-0003). */
    private UUID usuarioIdAutenticado() {
        if (identity.getPrincipal() instanceof JsonWebToken jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        throw new IllegalStateException("Token autenticado não é um JWT com claim 'sub'");
    }
}
