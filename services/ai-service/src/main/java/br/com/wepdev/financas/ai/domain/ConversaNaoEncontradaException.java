package br.com.wepdev.financas.ai.domain;

import java.util.UUID;

/** Conversa inexistente OU que não pertence ao usuário autenticado — mesmo erro nos dois casos (evita IDOR). */
public class ConversaNaoEncontradaException extends RuntimeException {

    public ConversaNaoEncontradaException(UUID id) {
        super("Conversa não encontrada: " + id);
    }
}
