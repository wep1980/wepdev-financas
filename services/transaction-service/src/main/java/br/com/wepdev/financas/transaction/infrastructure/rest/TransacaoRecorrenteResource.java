package br.com.wepdev.financas.transaction.infrastructure.rest;

import br.com.wepdev.financas.transaction.application.BuscarTransacaoRecorrenteUseCase;
import br.com.wepdev.financas.transaction.application.CancelarTransacaoRecorrenteUseCase;
import br.com.wepdev.financas.transaction.application.CriarTransacaoRecorrenteCommand;
import br.com.wepdev.financas.transaction.application.CriarTransacaoRecorrenteUseCase;
import br.com.wepdev.financas.transaction.application.ListarTransacoesRecorrentesUseCase;
import br.com.wepdev.financas.transaction.application.ProximosVencimentosUseCase;
import br.com.wepdev.financas.transaction.domain.StatusTransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;
import br.com.wepdev.financas.transaction.infrastructure.rest.dto.CriarTransacaoRecorrenteRequest;
import br.com.wepdev.financas.transaction.infrastructure.rest.dto.ProximoVencimentoResponse;
import br.com.wepdev.financas.transaction.infrastructure.rest.dto.TransacaoRecorrenteResponse;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
@Path("/api/v1/transacoes-recorrentes")
public class TransacaoRecorrenteResource {

    private final CriarTransacaoRecorrenteUseCase criarTransacaoRecorrenteUseCase;
    private final ListarTransacoesRecorrentesUseCase listarTransacoesRecorrentesUseCase;
    private final BuscarTransacaoRecorrenteUseCase buscarTransacaoRecorrenteUseCase;
    private final CancelarTransacaoRecorrenteUseCase cancelarTransacaoRecorrenteUseCase;
    private final ProximosVencimentosUseCase proximosVencimentosUseCase;
    private final SecurityIdentity identity;

    public TransacaoRecorrenteResource(CriarTransacaoRecorrenteUseCase criarTransacaoRecorrenteUseCase,
                                        ListarTransacoesRecorrentesUseCase listarTransacoesRecorrentesUseCase,
                                        BuscarTransacaoRecorrenteUseCase buscarTransacaoRecorrenteUseCase,
                                        CancelarTransacaoRecorrenteUseCase cancelarTransacaoRecorrenteUseCase,
                                        ProximosVencimentosUseCase proximosVencimentosUseCase,
                                        SecurityIdentity identity) {
        this.criarTransacaoRecorrenteUseCase = criarTransacaoRecorrenteUseCase;
        this.listarTransacoesRecorrentesUseCase = listarTransacoesRecorrentesUseCase;
        this.buscarTransacaoRecorrenteUseCase = buscarTransacaoRecorrenteUseCase;
        this.cancelarTransacaoRecorrenteUseCase = cancelarTransacaoRecorrenteUseCase;
        this.proximosVencimentosUseCase = proximosVencimentosUseCase;
        this.identity = identity;
    }

    @POST
    @RolesAllowed("usuario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response criar(@Valid CriarTransacaoRecorrenteRequest request) {
        TransacaoRecorrente regra = criarTransacaoRecorrenteUseCase.executar(new CriarTransacaoRecorrenteCommand(
                request.contaId(),
                usuarioIdAutenticado(),
                request.descricao(),
                request.valor(),
                request.tipo(),
                request.categoria(),
                request.frequencia(),
                request.dataInicio(),
                request.quantidadeOcorrencias()
        ));
        return Response.status(Response.Status.CREATED)
                .entity(TransacaoRecorrenteResponse.de(regra))
                .build();
    }

    @GET
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TransacaoRecorrenteResponse> listar(@QueryParam("status") StatusTransacaoRecorrente status) {
        return listarTransacoesRecorrentesUseCase.executar(usuarioIdAutenticado(), status).stream()
                .map(TransacaoRecorrenteResponse::de)
                .toList();
    }

    @GET
    @Path("/proximos-vencimentos")
    @RolesAllowed("service")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProximoVencimentoResponse> proximosVencimentos(@NotNull @QueryParam("dias") Integer dias) {
        return proximosVencimentosUseCase.executar(LocalDate.now(), dias).stream()
                .map(ProximoVencimentoResponse::de)
                .toList();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public TransacaoRecorrenteResponse buscar(@PathParam("id") UUID id) {
        return TransacaoRecorrenteResponse.de(buscarTransacaoRecorrenteUseCase.executar(id, usuarioIdAutenticado()));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("usuario")
    public Response cancelar(@PathParam("id") UUID id) {
        cancelarTransacaoRecorrenteUseCase.executar(id, usuarioIdAutenticado());
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
