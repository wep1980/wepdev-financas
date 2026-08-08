package br.com.wepdev.financas.transaction.application;

import java.math.BigDecimal;

/** totalGastoPeriodoAnterior é nulo quando a categoria não teve gasto no período anterior de mesma duração. */
public record ResumoCategoria(
        String categoria,
        BigDecimal totalGasto,
        BigDecimal percentualDoTotal,
        BigDecimal totalGastoPeriodoAnterior
) {
}
