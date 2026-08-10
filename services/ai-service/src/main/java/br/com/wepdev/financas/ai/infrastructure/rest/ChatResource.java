package br.com.wepdev.financas.ai.infrastructure.rest;

import br.com.wepdev.financas.ai.application.AgenteOrquestradorUseCase;
import br.com.wepdev.financas.ai.application.ChatComando;
import br.com.wepdev.financas.ai.application.ChatResultado;
import br.com.wepdev.financas.ai.infrastructure.rest.dto.ChatRequest;
import br.com.wepdev.financas.ai.infrastructure.rest.dto.ChatResponse;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

/** Conforme docs/specs/ai-service.yaml — spec-driven, contrato antes do código. */
@Path("/api/v1/chat")
public class ChatResource {

    private final AgenteOrquestradorUseCase agenteOrquestradorUseCase;
    private final SecurityIdentity identity;

    public ChatResource(AgenteOrquestradorUseCase agenteOrquestradorUseCase, SecurityIdentity identity) {
        this.agenteOrquestradorUseCase = agenteOrquestradorUseCase;
        this.identity = identity;
    }

    @POST
    @RolesAllowed("usuario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ChatResponse chat(@Valid ChatRequest request) {
        ChatResultado resultado = agenteOrquestradorUseCase.executar(
                new ChatComando(usuarioIdAutenticado(), request.conversaId(), request.mensagem()));
        return ChatResponse.de(resultado);
    }

    /** sub do token OIDC = id do usuário no Keycloak — nunca aceitar usuarioId vindo do cliente (ADR-0003). */
    private UUID usuarioIdAutenticado() {
        if (identity.getPrincipal() instanceof JsonWebToken jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        throw new IllegalStateException("Token autenticado não é um JWT com claim 'sub'");
    }
}
