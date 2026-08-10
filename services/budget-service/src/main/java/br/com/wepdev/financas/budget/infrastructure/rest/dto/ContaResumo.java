package br.com.wepdev.financas.budget.infrastructure.rest.dto;

import br.com.wepdev.financas.budget.domain.Conta;

import java.math.BigDecimal;
import java.util.UUID;

public record ContaResumo(UUID contaId, String nome, String tipo, BigDecimal saldo) {
    static ContaResumo de(Conta conta) {
        return new ContaResumo(conta.id(), conta.nome(), conta.tipo(), conta.saldo());
    }
}
