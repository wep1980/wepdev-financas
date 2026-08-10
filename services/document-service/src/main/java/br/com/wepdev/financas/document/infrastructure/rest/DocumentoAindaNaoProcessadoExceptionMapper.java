package br.com.wepdev.financas.document.infrastructure.rest;

import br.com.wepdev.financas.document.domain.DocumentoAindaNaoProcessadoException;
import br.com.wepdev.financas.document.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DocumentoAindaNaoProcessadoExceptionMapper implements ExceptionMapper<DocumentoAindaNaoProcessadoException> {

    @Override
    public Response toResponse(DocumentoAindaNaoProcessadoException exception) {
        return Response.status(422)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
