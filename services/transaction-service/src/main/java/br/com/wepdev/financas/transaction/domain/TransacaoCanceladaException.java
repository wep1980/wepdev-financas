package br.com.wepdev.financas.transaction.domain;

import java.util.UUID;

/** Editar uma transação cancelada não faz sentido — ela não tem mais efeito ativo no saldo pra ajustar. */
public class TransacaoCanceladaException extends RuntimeException {

    public TransacaoCanceladaException(UUID id) {
        super("Transação já cancelada, não pode ser editada: " + id);
    }
}
