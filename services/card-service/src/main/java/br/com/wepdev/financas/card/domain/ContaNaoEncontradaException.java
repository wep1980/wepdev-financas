package br.com.wepdev.financas.card.domain;

import java.util.UUID;

/** contaPagamentoId inexistente OU que não pertence ao usuário autenticado — mesmo erro nos dois casos (evita IDOR, ver account-service). */
public class ContaNaoEncontradaException extends RuntimeException {

    public ContaNaoEncontradaException(UUID contaId) {
        super("Conta não encontrada: " + contaId);
    }
}
