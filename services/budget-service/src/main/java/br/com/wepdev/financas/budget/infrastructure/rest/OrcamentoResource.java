package br.com.wepdev.financas.budget.infrastructure.rest;

import br.com.wepdev.financas.budget.application.AtualizarOrcamentoCommand;
import br.com.wepdev.financas.budget.application.AtualizarOrcamentoUseCase;
import br.com.wepdev.financas.budget.application.CriarOrcamentoCommand;
import br.com.wepdev.financas.budget.application.CriarOrcamentoUseCase;
import br.com.wepdev.financas.budget.application.ExcluirOrcamentoUseCase;
import br.com.wepdev.financas.budget.application.ListarOrcamentosUseCase;
import br.com.wepdev.financas.budget.application.OrcamentoDetalhe;
import br.com.wepdev.financas.budget.infrastructure.rest.dto.AtualizarOrcamentoRequest;
import br.com.wepdev.financas.budget.infrastructure.rest.dto.CriarOrcamentoRequest;
import br.com.wepdev.financas.budget.infrastructure.rest.dto.OrcamentoResponse;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Conforme docs/specs/budget-service.yaml — spec-driven, contrato antes do código. */
@Path("/api/v1/orcamentos")
public class OrcamentoResource {

    private final CriarOrcamentoUseCase criarOrcamentoUseCase;
    private final ListarOrcamentosUseCase listarOrcamentosUseCase;
    private final AtualizarOrcamentoUseCase atualizarOrcamentoUseCase;
    private final ExcluirOrcamentoUseCase excluirOrcamentoUseCase;
    private final SecurityIdentity identity;

    public OrcamentoResource(CriarOrcamentoUseCase criarOrcamentoUseCase, ListarOrcamentosUseCase listarOrcamentosUseCase,
                              AtualizarOrcamentoUseCase atualizarOrcamentoUseCase, ExcluirOrcamentoUseCase excluirOrcamentoUseCase,
                              SecurityIdentity identity) {
        this.criarOrcamentoUseCase = criarOrcamentoUseCase;
        this.listarOrcamentosUseCase = listarOrcamentosUseCase;
        this.atualizarOrcamentoUseCase = atualizarOrcamentoUseCase;
        this.excluirOrcamentoUseCase = excluirOrcamentoUseCase;
        this.identity = identity;
    }

    @POST
    @RolesAllowed("usuario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response criar(@Valid CriarOrcamentoRequest request) {
        OrcamentoDetalhe detalhe = criarOrcamentoUseCase.executar(new CriarOrcamentoCommand(
                usuarioIdAutenticado(), request.categoria(), request.mesReferencia(), request.valorLimite()));
        return Response.status(Response.Status.CREATED)
                .entity(OrcamentoResponse.de(detalhe))
                .build();
    }

    @GET
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OrcamentoResponse> listar(
            @NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$") @QueryParam("mes") String mes) {
        return listarOrcamentosUseCase.executar(usuarioIdAutenticado(), YearMonth.parse(mes)).stream()
                .map(OrcamentoResponse::de)
                .toList();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("usuario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public OrcamentoResponse atualizar(@PathParam("id") UUID id, @Valid AtualizarOrcamentoRequest request) {
        OrcamentoDetalhe detalhe = atualizarOrcamentoUseCase.executar(
                new AtualizarOrcamentoCommand(id, usuarioIdAutenticado(), request.valorLimite()));
        return OrcamentoResponse.de(detalhe);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("usuario")
    public Response excluir(@PathParam("id") UUID id) {
        excluirOrcamentoUseCase.executar(id, usuarioIdAutenticado());
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
