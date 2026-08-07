package br.com.wepdev.financas.account.infrastructure.rest;

import br.com.wepdev.financas.account.domain.ContaNaoEncontradaException;
import br.com.wepdev.financas.account.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ContaNaoEncontradaExceptionMapper implements ExceptionMapper<ContaNaoEncontradaException> {

    @Override
    public Response toResponse(ContaNaoEncontradaException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
