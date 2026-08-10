package br.com.wepdev.financas.budget.infrastructure.rest;

import br.com.wepdev.financas.budget.domain.OrcamentoNaoEncontradoException;
import br.com.wepdev.financas.budget.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OrcamentoNaoEncontradoExceptionMapper implements ExceptionMapper<OrcamentoNaoEncontradoException> {

    @Override
    public Response toResponse(OrcamentoNaoEncontradoException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
