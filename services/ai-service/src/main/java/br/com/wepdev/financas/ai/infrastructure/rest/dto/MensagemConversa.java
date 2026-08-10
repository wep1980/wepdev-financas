package br.com.wepdev.financas.ai.infrastructure.rest.dto;

import br.com.wepdev.financas.ai.domain.Mensagem;

import java.time.Instant;

public record MensagemConversa(String autor, String texto, String tipo, Instant criadaEm) {
    public static MensagemConversa de(Mensagem mensagem) {
        return new MensagemConversa(
                mensagem.getAutor().name(),
                mensagem.getTexto(),
                mensagem.getTipo() == null ? null : mensagem.getTipo().name(),
                mensagem.getCriadaEm()
        );
    }
}
