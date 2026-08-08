package br.com.wepdev.financas.transaction.domain;

import java.util.UUID;

/** Regra inexistente OU que não pertence ao usuário autenticado — mesmo erro nos dois casos (evita IDOR). */
public class TransacaoRecorrenteNaoEncontradaException extends RuntimeException {

    public TransacaoRecorrenteNaoEncontradaException(UUID id) {
        super("Transação recorrente não encontrada: " + id);
    }
}
