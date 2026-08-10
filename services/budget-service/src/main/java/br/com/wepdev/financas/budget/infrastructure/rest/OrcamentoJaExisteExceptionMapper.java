package br.com.wepdev.financas.budget.infrastructure.rest;

import br.com.wepdev.financas.budget.domain.OrcamentoJaExisteException;
import br.com.wepdev.financas.budget.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OrcamentoJaExisteExceptionMapper implements ExceptionMapper<OrcamentoJaExisteException> {

    @Override
    public Response toResponse(OrcamentoJaExisteException exception) {
        return Response.status(422)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
