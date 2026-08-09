package br.com.wepdev.financas.card.infrastructure.rest;

import br.com.wepdev.financas.card.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.card.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CartaoNaoEncontradoExceptionMapper implements ExceptionMapper<CartaoNaoEncontradoException> {

    @Override
    public Response toResponse(CartaoNaoEncontradoException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
