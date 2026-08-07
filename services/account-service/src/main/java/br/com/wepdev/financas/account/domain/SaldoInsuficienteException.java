package br.com.wepdev.financas.account.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(UUID contaId, BigDecimal saldoAtual, BigDecimal valorSolicitado) {
        super("Saldo insuficiente na conta %s: saldo=%s, solicitado=%s"
                .formatted(contaId, saldoAtual, valorSolicitado));
    }
}
