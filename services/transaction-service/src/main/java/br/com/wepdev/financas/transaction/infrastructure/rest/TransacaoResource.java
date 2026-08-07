package br.com.wepdev.financas.transaction.infrastructure.rest;

import br.com.wepdev.financas.transaction.application.CancelarTransacaoUseCase;
import br.com.wepdev.financas.transaction.application.ListarTransacoesUseCase;
import br.com.wepdev.financas.transaction.application.RegistrarTransacaoCommand;
import br.com.wepdev.financas.transaction.application.RegistrarTransacaoUseCase;
import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoFiltro;
import br.com.wepdev.financas.transaction.infrastructure.rest.dto.CriarTransacaoRequest;
import br.com.wepdev.financas.transaction.infrastructure.rest.dto.TransacaoResponse;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

/** Conforme docs/specs/transaction-service.yaml — spec-driven, contrato antes do código. */
@Path("/api/v1/transacoes")
public class TransacaoResource {

    private final RegistrarTransacaoUseCase registrarTransacaoUseCase;
    private final ListarTransacoesUseCase listarTransacoesUseCase;
    private final CancelarTransacaoUseCase cancelarTransacaoUseCase;
    private final SecurityIdentity identity;

    public TransacaoResource(RegistrarTransacaoUseCase registrarTransacaoUseCase,
                              ListarTransacoesUseCase listarTransacoesUseCase,
                              CancelarTransacaoUseCase cancelarTransacaoUseCase, SecurityIdentity identity) {
        this.registrarTransacaoUseCase = registrarTransacaoUseCase;
        this.listarTransacoesUseCase = listarTransacoesUseCase;
        this.cancelarTransacaoUseCase = cancelarTransacaoUseCase;
        this.identity = identity;
    }

    @POST
    @RolesAllowed("usuario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrar(@Valid CriarTransacaoRequest request) {
        Transacao transacao = registrarTransacaoUseCase.executar(new RegistrarTransacaoCommand(
                request.contaId(),
                usuarioIdAutenticado(),
                request.descricao(),
                request.valor(),
                request.tipo(),
                request.categoria(),
                request.dataTransacao()
        ));
        return Response.status(Response.Status.CREATED)
                .entity(TransacaoResponse.de(transacao))
                .build();
    }

    @GET
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TransacaoResponse> listar(@QueryParam("contaId") UUID contaId,
                                           @QueryParam("inicio") LocalDate inicio,
                                           @QueryParam("fim") LocalDate fim) {
        TransacaoFiltro filtro = new TransacaoFiltro(usuarioIdAutenticado(), contaId, inicio, fim);
        return listarTransacoesUseCase.executar(filtro).stream()
                .map(TransacaoResponse::de)
                .toList();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("usuario")
    public Response cancelar(@PathParam("id") UUID id) {
        cancelarTransacaoUseCase.executar(id, usuarioIdAutenticado());
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
