package br.com.wepdev.financas.card.infrastructure.rest;

import br.com.wepdev.financas.card.application.AtualizarCartaoCommand;
import br.com.wepdev.financas.card.application.AtualizarCartaoUseCase;
import br.com.wepdev.financas.card.application.BuscarCartaoUseCase;
import br.com.wepdev.financas.card.application.CriarCartaoCommand;
import br.com.wepdev.financas.card.application.CriarCartaoUseCase;
import br.com.wepdev.financas.card.application.ExcluirCartaoUseCase;
import br.com.wepdev.financas.card.application.ListarCartoesUseCase;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.infrastructure.rest.dto.AtualizarCartaoRequest;
import br.com.wepdev.financas.card.infrastructure.rest.dto.CartaoResponse;
import br.com.wepdev.financas.card.infrastructure.rest.dto.CriarCartaoRequest;
import io.quarkus.security.identity.SecurityIdentity;
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
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

/** Conforme docs/specs/card-service.yaml — spec-driven, contrato antes do código. */
@Path("/api/v1/cartoes")
public class CartaoResource {

    private final CriarCartaoUseCase criarCartaoUseCase;
    private final ListarCartoesUseCase listarCartoesUseCase;
    private final BuscarCartaoUseCase buscarCartaoUseCase;
    private final AtualizarCartaoUseCase atualizarCartaoUseCase;
    private final ExcluirCartaoUseCase excluirCartaoUseCase;
    private final SecurityIdentity identity;

    public CartaoResource(CriarCartaoUseCase criarCartaoUseCase, ListarCartoesUseCase listarCartoesUseCase,
                           BuscarCartaoUseCase buscarCartaoUseCase, AtualizarCartaoUseCase atualizarCartaoUseCase,
                           ExcluirCartaoUseCase excluirCartaoUseCase, SecurityIdentity identity) {
        this.criarCartaoUseCase = criarCartaoUseCase;
        this.listarCartoesUseCase = listarCartoesUseCase;
        this.buscarCartaoUseCase = buscarCartaoUseCase;
        this.atualizarCartaoUseCase = atualizarCartaoUseCase;
        this.excluirCartaoUseCase = excluirCartaoUseCase;
        this.identity = identity;
    }

    @POST
    @RolesAllowed("usuario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response criar(@Valid CriarCartaoRequest request) {
        Cartao cartao = criarCartaoUseCase.executar(new CriarCartaoCommand(
                usuarioIdAutenticado(),
                request.apelido(),
                request.bandeira(),
                request.limite(),
                request.diaFechamento(),
                request.diaVencimento(),
                request.contaPagamentoId()
        ));
        return Response.status(Response.Status.CREATED)
                .entity(CartaoResponse.de(cartao))
                .build();
    }

    @GET
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public List<CartaoResponse> listar() {
        return listarCartoesUseCase.executar(usuarioIdAutenticado()).stream()
                .map(CartaoResponse::de)
                .toList();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public CartaoResponse buscarPorId(@PathParam("id") UUID id) {
        return CartaoResponse.de(buscarCartaoUseCase.executar(id, usuarioIdAutenticado()));
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("usuario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CartaoResponse atualizar(@PathParam("id") UUID id, @Valid AtualizarCartaoRequest request) {
        Cartao cartao = atualizarCartaoUseCase.executar(new AtualizarCartaoCommand(
                id, usuarioIdAutenticado(), request.apelido(), request.bandeira(), request.limite(),
                request.diaFechamento(), request.diaVencimento(), request.contaPagamentoId()
        ));
        return CartaoResponse.de(cartao);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("usuario")
    public Response excluir(@PathParam("id") UUID id) {
        excluirCartaoUseCase.executar(id, usuarioIdAutenticado());
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
