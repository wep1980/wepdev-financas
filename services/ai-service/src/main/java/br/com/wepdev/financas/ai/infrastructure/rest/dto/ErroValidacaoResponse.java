package br.com.wepdev.financas.ai.infrastructure.rest.dto;

import java.util.List;

public record ErroValidacaoResponse(String mensagem, List<CampoErro> erros) {

    public record CampoErro(String campo, String mensagem) {
    }
}
