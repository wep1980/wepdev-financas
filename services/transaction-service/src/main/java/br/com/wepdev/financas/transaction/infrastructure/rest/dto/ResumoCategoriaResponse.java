package br.com.wepdev.financas.transaction.infrastructure.rest.dto;

import br.com.wepdev.financas.transaction.application.ResumoCategoria;

import java.math.BigDecimal;

public record ResumoCategoriaResponse(
        String categoria,
        BigDecimal totalGasto,
        BigDecimal percentualDoTotal,
        BigDecimal totalGastoPeriodoAnterior
) {
    public static ResumoCategoriaResponse de(ResumoCategoria resumo) {
        return new ResumoCategoriaResponse(
                resumo.categoria(),
                resumo.totalGasto(),
                resumo.percentualDoTotal(),
                resumo.totalGastoPeriodoAnterior()
        );
    }
}
