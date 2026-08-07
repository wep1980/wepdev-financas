package br.com.wepdev.financas.account.infrastructure.rest;

import br.com.wepdev.financas.account.domain.SaldoInsuficienteException;
import br.com.wepdev.financas.account.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SaldoInsuficienteExceptionMapper implements ExceptionMapper<SaldoInsuficienteException> {

    @Override
    public Response toResponse(SaldoInsuficienteException exception) {
        return Response.status(422)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
