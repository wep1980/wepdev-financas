package br.com.wepdev.financas.transaction.domain;

import java.util.UUID;

/** Transação inexistente OU que não pertence ao usuário autenticado — mesmo erro nos dois casos (evita IDOR, ver account-service). */
public class TransacaoNaoEncontradaException extends RuntimeException {

    public TransacaoNaoEncontradaException(UUID id) {
        super("Transação não encontrada: " + id);
    }
}
