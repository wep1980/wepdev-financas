package br.com.wepdev.financas.card.domain;

import java.util.UUID;

/** contaPagamentoId não tem saldo suficiente pra pagar a fatura. */
public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(UUID contaId) {
        super("Saldo insuficiente na conta: " + contaId);
    }
}
