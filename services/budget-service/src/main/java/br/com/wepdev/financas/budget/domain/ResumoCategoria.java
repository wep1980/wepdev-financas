package br.com.wepdev.financas.budget.domain;

import java.math.BigDecimal;

/** Leitura do transaction-service (GET /transacoes/resumo-por-categoria) — só o que Orcamento.valorConsumido precisa. */
public record ResumoCategoria(String categoria, BigDecimal totalGasto) {
}
