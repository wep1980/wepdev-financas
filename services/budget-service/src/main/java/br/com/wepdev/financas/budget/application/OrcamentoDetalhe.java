package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.Orcamento;

import java.math.BigDecimal;

/** valorConsumido é calculado na hora (transaction-service), nunca persistido — ver ADR-0026. */
public record OrcamentoDetalhe(Orcamento orcamento, BigDecimal valorConsumido) {
}
