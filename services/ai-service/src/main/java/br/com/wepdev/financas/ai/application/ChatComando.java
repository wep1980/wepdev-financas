package br.com.wepdev.financas.ai.application;

import java.util.UUID;

/** conversaId nulo = inicia conversa nova. */
public record ChatComando(UUID usuarioId, UUID conversaId, String mensagem) {
}
