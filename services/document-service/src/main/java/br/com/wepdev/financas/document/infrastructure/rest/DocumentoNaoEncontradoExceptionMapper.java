package br.com.wepdev.financas.document.infrastructure.rest;

import br.com.wepdev.financas.document.domain.DocumentoNaoEncontradoException;
import br.com.wepdev.financas.document.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DocumentoNaoEncontradoExceptionMapper implements ExceptionMapper<DocumentoNaoEncontradoException> {

    @Override
    public Response toResponse(DocumentoNaoEncontradoException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
