package br.com.wepdev.financas.ai.infrastructure.rest.dto;

import br.com.wepdev.financas.ai.domain.Conversa;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversaDetalheResponse(UUID id, Instant iniciadaEm, List<MensagemConversa> mensagens) {
    public static ConversaDetalheResponse de(Conversa conversa) {
        return new ConversaDetalheResponse(
                conversa.getId(),
                conversa.getIniciadaEm(),
                conversa.getMensagens().stream().map(MensagemConversa::de).toList()
        );
    }
}
