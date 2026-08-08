package br.com.wepdev.financas.transaction.infrastructure.rest;

import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteNaoEncontradaException;
import br.com.wepdev.financas.transaction.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TransacaoRecorrenteNaoEncontradaExceptionMapper implements ExceptionMapper<TransacaoRecorrenteNaoEncontradaException> {

    @Override
    public Response toResponse(TransacaoRecorrenteNaoEncontradaException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
