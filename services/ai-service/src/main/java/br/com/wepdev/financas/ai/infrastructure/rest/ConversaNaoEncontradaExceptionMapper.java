package br.com.wepdev.financas.ai.infrastructure.rest;

import br.com.wepdev.financas.ai.domain.ConversaNaoEncontradaException;
import br.com.wepdev.financas.ai.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConversaNaoEncontradaExceptionMapper implements ExceptionMapper<ConversaNaoEncontradaException> {

    @Override
    public Response toResponse(ConversaNaoEncontradaException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
