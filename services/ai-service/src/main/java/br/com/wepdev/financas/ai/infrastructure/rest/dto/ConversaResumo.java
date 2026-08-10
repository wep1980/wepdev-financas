package br.com.wepdev.financas.ai.infrastructure.rest.dto;

import br.com.wepdev.financas.ai.domain.Conversa;
import br.com.wepdev.financas.ai.domain.Mensagem;

import java.time.Instant;
import java.util.UUID;

public record ConversaResumo(UUID id, Instant iniciadaEm, Instant ultimaMensagemEm, String ultimaMensagemPreview) {

    private static final int TAMANHO_PREVIEW = 120;

    public static ConversaResumo de(Conversa conversa) {
        String preview = conversa.getMensagens().isEmpty()
                ? ""
                : previewDe(conversa.getMensagens().get(conversa.getMensagens().size() - 1));
        return new ConversaResumo(conversa.getId(), conversa.getIniciadaEm(), conversa.getUltimaAtividadeEm(), preview);
    }

    private static String previewDe(Mensagem ultima) {
        String texto = ultima.getTexto();
        return texto.length() > TAMANHO_PREVIEW ? texto.substring(0, TAMANHO_PREVIEW) + "…" : texto;
    }
}
