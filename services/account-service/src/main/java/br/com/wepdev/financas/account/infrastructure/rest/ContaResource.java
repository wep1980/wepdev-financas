package br.com.wepdev.financas.account.infrastructure.rest;

import br.com.wepdev.financas.account.application.AtualizarContaCommand;
import br.com.wepdev.financas.account.application.AtualizarContaUseCase;
import br.com.wepdev.financas.account.application.BuscarContaUseCase;
import br.com.wepdev.financas.account.application.CreditarSaldoUseCase;
import br.com.wepdev.financas.account.application.CriarContaCommand;
import br.com.wepdev.financas.account.application.CriarContaUseCase;
import br.com.wepdev.financas.account.application.DebitarSaldoUseCase;
import br.com.wepdev.financas.account.application.ExcluirContaUseCase;
import br.com.wepdev.financas.account.application.ListarContasUseCase;
import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.infrastructure.rest.dto.AjusteSaldoRequest;
import br.com.wepdev.financas.account.infrastructure.rest.dto.AtualizarContaRequest;
import br.com.wepdev.financas.account.infrastructure.rest.dto.ContaResponse;
import br.com.wepdev.financas.account.infrastructure.rest.dto.CriarContaRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

/** Conforme docs/specs/account-service.yaml — spec-driven, contrato antes do código. */
@Path("/api/v1/contas")
public class ContaResource {

    private final CriarContaUseCase criarContaUseCase;
    private final ListarContasUseCase listarContasUseCase;
    private final BuscarContaUseCase buscarContaUseCase;
    private final AtualizarContaUseCase atualizarContaUseCase;
    private final ExcluirContaUseCase excluirContaUseCase;
    private final DebitarSaldoUseCase debitarSaldoUseCase;
    private final CreditarSaldoUseCase creditarSaldoUseCase;
    private final SecurityIdentity identity;

    public ContaResource(CriarContaUseCase criarContaUseCase, ListarContasUseCase listarContasUseCase,
                          BuscarContaUseCase buscarContaUseCase, AtualizarContaUseCase atualizarContaUseCase,
                          ExcluirContaUseCase excluirContaUseCase, DebitarSaldoUseCase debitarSaldoUseCase,
                          CreditarSaldoUseCase creditarSaldoUseCase, SecurityIdentity identity) {
        this.criarContaUseCase = criarContaUseCase;
        this.listarContasUseCase = listarContasUseCase;
        this.buscarContaUseCase = buscarContaUseCase;
        this.atualizarContaUseCase = atualizarContaUseCase;
        this.excluirContaUseCase = excluirContaUseCase;
        this.debitarSaldoUseCase = debitarSaldoUseCase;
        this.creditarSaldoUseCase = creditarSaldoUseCase;
        this.identity = identity;
    }

    @POST
    @RolesAllowed("usuario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response criar(@Valid CriarContaRequest request) {
        Conta conta = criarContaUseCase.executar(new CriarContaCommand(
                usuarioIdAutenticado(),
                request.nome(),
                request.tipo(),
                request.saldoInicial(),
                request.instituicao()
        ));
        return Response.status(Response.Status.CREATED)
                .entity(ContaResponse.de(conta))
                .build();
    }

    @GET
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ContaResponse> listar() {
        return listarContasUseCase.executar(usuarioIdAutenticado()).stream()
                .map(ContaResponse::de)
                .toList();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public ContaResponse buscarPorId(@PathParam("id") UUID id) {
        return ContaResponse.de(buscarContaUseCase.executar(id, usuarioIdAutenticado()));
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("usuario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ContaResponse atualizar(@PathParam("id") UUID id, @Valid AtualizarContaRequest request) {
        Conta conta = atualizarContaUseCase.executar(new AtualizarContaCommand(
                id, usuarioIdAutenticado(), request.nome(), request.instituicao()
        ));
        return ContaResponse.de(conta);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("usuario")
    public Response excluir(@PathParam("id") UUID id) {
        excluirContaUseCase.executar(id, usuarioIdAutenticado());
        return Response.noContent().build();
    }

    /** Endpoint interno (ADR-0003) — chamado pelo transaction-service, nunca exposto ao front-end. */
    @POST
    @Path("/{id}/debitos")
    @RolesAllowed("service")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ContaResponse debitar(@PathParam("id") UUID id, @Valid AjusteSaldoRequest request) {
        return ContaResponse.de(debitarSaldoUseCase.executar(id, request.valor()));
    }

    /** Endpoint interno (ADR-0003) — chamado pelo transaction-service, nunca exposto ao front-end. */
    @POST
    @Path("/{id}/creditos")
    @RolesAllowed("service")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ContaResponse creditar(@PathParam("id") UUID id, @Valid AjusteSaldoRequest request) {
        return ContaResponse.de(creditarSaldoUseCase.executar(id, request.valor()));
    }

    /** sub do token OIDC = id do usuário no Keycloak — nunca aceitar usuarioId vindo do cliente (ADR-0003). */
    private UUID usuarioIdAutenticado() {
        if (identity.getPrincipal() instanceof JsonWebToken jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        throw new IllegalStateException("Token autenticado não é um JWT com claim 'sub'");
    }
}
