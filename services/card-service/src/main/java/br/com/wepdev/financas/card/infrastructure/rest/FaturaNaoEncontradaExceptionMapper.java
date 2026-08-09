package br.com.wepdev.financas.card.infrastructure.rest;

import br.com.wepdev.financas.card.domain.FaturaNaoEncontradaException;
import br.com.wepdev.financas.card.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class FaturaNaoEncontradaExceptionMapper implements ExceptionMapper<FaturaNaoEncontradaException> {

    @Override
    public Response toResponse(FaturaNaoEncontradaException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
