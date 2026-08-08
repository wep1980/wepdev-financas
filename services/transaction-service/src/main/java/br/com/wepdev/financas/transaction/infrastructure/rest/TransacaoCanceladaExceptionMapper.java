package br.com.wepdev.financas.transaction.infrastructure.rest;

import br.com.wepdev.financas.transaction.domain.TransacaoCanceladaException;
import br.com.wepdev.financas.transaction.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TransacaoCanceladaExceptionMapper implements ExceptionMapper<TransacaoCanceladaException> {

    @Override
    public Response toResponse(TransacaoCanceladaException exception) {
        return Response.status(422)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
