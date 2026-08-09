package br.com.wepdev.financas.card.infrastructure.rest;

import br.com.wepdev.financas.card.domain.FaturaAindaAbertaException;
import br.com.wepdev.financas.card.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class FaturaAindaAbertaExceptionMapper implements ExceptionMapper<FaturaAindaAbertaException> {

    @Override
    public Response toResponse(FaturaAindaAbertaException exception) {
        return Response.status(422)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
