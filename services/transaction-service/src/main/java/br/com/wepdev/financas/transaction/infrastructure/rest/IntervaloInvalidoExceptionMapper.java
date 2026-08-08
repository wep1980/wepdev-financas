package br.com.wepdev.financas.transaction.infrastructure.rest;

import br.com.wepdev.financas.transaction.domain.IntervaloInvalidoException;
import br.com.wepdev.financas.transaction.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class IntervaloInvalidoExceptionMapper implements ExceptionMapper<IntervaloInvalidoException> {

    @Override
    public Response toResponse(IntervaloInvalidoException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
