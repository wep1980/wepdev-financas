package br.com.wepdev.financas.ai.infrastructure.rest;

import br.com.wepdev.financas.ai.domain.IaNaoConfiguradaException;
import br.com.wepdev.financas.ai.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class IaNaoConfiguradaExceptionMapper implements ExceptionMapper<IaNaoConfiguradaException> {

    @Override
    public Response toResponse(IaNaoConfiguradaException exception) {
        return Response.status(422)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
