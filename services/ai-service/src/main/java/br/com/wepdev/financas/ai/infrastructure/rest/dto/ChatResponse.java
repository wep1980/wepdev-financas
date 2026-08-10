package br.com.wepdev.financas.ai.infrastructure.rest.dto;

import br.com.wepdev.financas.ai.application.ChatResultado;

import java.util.List;
import java.util.UUID;

public record ChatResponse(
        UUID conversaId,
        String resposta,
        String tipo,
        AcaoProposta acaoProposta,
        List<ToolInvocada> trace
) {
    public static ChatResponse de(ChatResultado resultado) {
        return new ChatResponse(
                resultado.conversaId(),
                resultado.resposta(),
                resultado.tipo().name(),
                resultado.acaoProposta() == null ? null : AcaoProposta.de(resultado.acaoProposta()),
                resultado.trace().stream().map(ToolInvocada::de).toList()
        );
    }
}
