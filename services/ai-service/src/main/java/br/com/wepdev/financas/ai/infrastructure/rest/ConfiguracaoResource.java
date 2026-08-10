package br.com.wepdev.financas.ai.infrastructure.rest;

import br.com.wepdev.financas.ai.application.BuscarConfiguracaoIaUseCase;
import br.com.wepdev.financas.ai.application.DefinirConfiguracaoIaComando;
import br.com.wepdev.financas.ai.application.DefinirConfiguracaoIaUseCase;
import br.com.wepdev.financas.ai.domain.ConfiguracaoIa;
import br.com.wepdev.financas.ai.infrastructure.rest.dto.ConfiguracaoIaRequest;
import br.com.wepdev.financas.ai.infrastructure.rest.dto.ConfiguracaoIaResponse;
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

/** Conforme docs/specs/ai-service.yaml — spec-driven, contrato antes do código. */
@Path("/api/v1/configuracao")
public class ConfiguracaoResource {

    private final BuscarConfiguracaoIaUseCase buscarConfiguracaoIaUseCase;
    private final DefinirConfiguracaoIaUseCase definirConfiguracaoIaUseCase;
    private final SecurityIdentity identity;

    public ConfiguracaoResource(BuscarConfiguracaoIaUseCase buscarConfiguracaoIaUseCase,
                                 DefinirConfiguracaoIaUseCase definirConfiguracaoIaUseCase, SecurityIdentity identity) {
        this.buscarConfiguracaoIaUseCase = buscarConfiguracaoIaUseCase;
        this.definirConfiguracaoIaUseCase = definirConfiguracaoIaUseCase;
        this.identity = identity;
    }

    @GET
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public ConfiguracaoIaResponse buscar() {
        return ConfiguracaoIaResponse.de(buscarConfiguracaoIaUseCase.executar(usuarioIdAutenticado()));
    }

    @PUT
    @RolesAllowed("usuario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ConfiguracaoIaResponse definir(@Valid ConfiguracaoIaRequest request) {
        ConfiguracaoIa configuracao = definirConfiguracaoIaUseCase.executar(new DefinirConfiguracaoIaComando(
                usuarioIdAutenticado(), request.provedor(), request.apiKey(), request.ollamaUrl()));
        return ConfiguracaoIaResponse.de(configuracao);
    }

    /** sub do token OIDC = id do usuário no Keycloak — nunca aceitar usuarioId vindo do cliente (ADR-0003). */
    private UUID usuarioIdAutenticado() {
        if (identity.getPrincipal() instanceof JsonWebToken jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        throw new IllegalStateException("Token autenticado não é um JWT com claim 'sub'");
    }
}
