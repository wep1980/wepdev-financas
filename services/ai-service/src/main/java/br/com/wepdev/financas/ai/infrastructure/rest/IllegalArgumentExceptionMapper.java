package br.com.wepdev.financas.ai.infrastructure.rest;

import br.com.wepdev.financas.ai.infrastructure.rest.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Cobre validação de regra de negócio que o Bean Validation da request não
 * consegue expressar sozinho (campo cruzado, ex: apiKey obrigatória só
 * quando provedor=OPENAI — ConfiguracaoIa.validarProvedor). Mesmo
 * princípio dos outros ErroValidacaoResponse/ErroResponse, só que pra
 * uma exceção que nasce no domínio, não na anotação da request.
 */
@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErroResponse(exception.getMessage()))
                .build();
    }
}
