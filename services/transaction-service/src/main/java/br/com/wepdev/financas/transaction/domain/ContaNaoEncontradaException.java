package br.com.wepdev.financas.transaction.domain;

import java.util.UUID;

/** Conta inexistente OU que não pertence ao usuário autenticado — mesmo erro nos dois casos (evita IDOR, ver account-service). */
public class ContaNaoEncontradaException extends RuntimeException {

    public ContaNaoEncontradaException(UUID contaId) {
        super("Conta não encontrada: " + contaId);
    }
}
