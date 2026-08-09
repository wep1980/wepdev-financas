package br.com.wepdev.financas.card.domain;

import java.util.UUID;

/** Fatura inexistente OU que não pertence ao usuário autenticado — mesmo erro nos dois casos (evita IDOR). */
public class FaturaNaoEncontradaException extends RuntimeException {

    public FaturaNaoEncontradaException(UUID id) {
        super("Fatura não encontrada: " + id);
    }
}
