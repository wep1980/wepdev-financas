package br.com.wepdev.financas.ai.domain;

import java.math.BigDecimal;

/** Leitura do transaction-service — tool MCP resumo_gastos_por_categoria (mesmo cálculo do dashboard/PRD 3.7). */
public record ResumoCategoria(String categoria, BigDecimal totalGasto, BigDecimal percentualDoTotal,
                               BigDecimal totalGastoPeriodoAnterior) {
}
