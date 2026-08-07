package br.com.wepdev.financas.account.infrastructure.rest;

import br.com.wepdev.financas.account.infrastructure.rest.dto.ErroValidacaoResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ErroValidacaoResponse.CampoErro> erros = exception.getConstraintViolations().stream()
                .map(this::paraCampoErro)
                .toList();
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErroValidacaoResponse("Erro de validação", erros))
                .build();
    }

    private ErroValidacaoResponse.CampoErro paraCampoErro(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String campo = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        return new ErroValidacaoResponse.CampoErro(campo, violation.getMessage());
    }
}
