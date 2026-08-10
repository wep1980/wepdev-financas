package br.com.wepdev.financas.document.infrastructure.rest;

import br.com.wepdev.financas.document.domain.NenhumLancamentoSelecionadoException;
import br.com.wepdev.financas.document.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NenhumLancamentoSelecionadoExceptionMapper implements ExceptionMapper<NenhumLancamentoSelecionadoException> {

    @Override
    public Response toResponse(NenhumLancamentoSelecionadoException exception) {
        return Response.status(422)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
