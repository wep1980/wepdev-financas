package br.com.wepdev.financas.transaction.domain;

import java.util.UUID;

public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(UUID contaId) {
        super("Saldo insuficiente na conta: " + contaId);
    }
}
