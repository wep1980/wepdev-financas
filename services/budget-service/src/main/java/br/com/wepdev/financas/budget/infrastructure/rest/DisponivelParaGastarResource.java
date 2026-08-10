package br.com.wepdev.financas.budget.infrastructure.rest;

import br.com.wepdev.financas.budget.application.CalcularDisponivelParaGastarUseCase;
import br.com.wepdev.financas.budget.infrastructure.rest.dto.DisponivelParaGastarResponse;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.YearMonth;
import java.util.UUID;

/** Conforme docs/specs/budget-service.yaml — spec-driven, contrato antes do código. */
@Path("/api/v1/disponivel-para-gastar")
public class DisponivelParaGastarResource {

    private final CalcularDisponivelParaGastarUseCase calcularDisponivelParaGastarUseCase;
    private final SecurityIdentity identity;

    public DisponivelParaGastarResource(CalcularDisponivelParaGastarUseCase calcularDisponivelParaGastarUseCase,
                                         SecurityIdentity identity) {
        this.calcularDisponivelParaGastarUseCase = calcularDisponivelParaGastarUseCase;
        this.identity = identity;
    }

    @GET
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public DisponivelParaGastarResponse calcular(
            @NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$") @QueryParam("mes") String mes) {
        return DisponivelParaGastarResponse.de(
                calcularDisponivelParaGastarUseCase.executar(usuarioIdAutenticado(), YearMonth.parse(mes)));
    }

    /** sub do token OIDC = id do usuário no Keycloak — nunca aceitar usuarioId vindo do cliente (ADR-0003). */
    private UUID usuarioIdAutenticado() {
        if (identity.getPrincipal() instanceof JsonWebToken jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        throw new IllegalStateException("Token autenticado não é um JWT com claim 'sub'");
    }
}
