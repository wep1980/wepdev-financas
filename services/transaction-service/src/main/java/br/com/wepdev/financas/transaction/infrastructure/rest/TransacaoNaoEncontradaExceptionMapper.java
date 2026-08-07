package br.com.wepdev.financas.transaction.infrastructure.rest;

import br.com.wepdev.financas.transaction.domain.TransacaoNaoEncontradaException;
import br.com.wepdev.financas.transaction.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TransacaoNaoEncontradaExceptionMapper implements ExceptionMapper<TransacaoNaoEncontradaException> {

    @Override
    public Response toResponse(TransacaoNaoEncontradaException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
