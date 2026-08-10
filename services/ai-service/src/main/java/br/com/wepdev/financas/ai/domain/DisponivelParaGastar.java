package br.com.wepdev.financas.ai.domain;

import java.math.BigDecimal;

/** Leitura do budget-service (ADR-0026) — já vem com o cálculo completo pronto, a tool buscar_saldo_disponivel só repassa. */
public record DisponivelParaGastar(BigDecimal valorDisponivel, BigDecimal saldoContas, BigDecimal faturasEmAberto,
                                    BigDecimal despesasRecorrentes, BigDecimal reserva) {
}
