package br.com.wepdev.financas.ai.infrastructure.client.dto;

import java.math.BigDecimal;

/** Só os campos que este serviço precisa da resposta do budget-service — não é o contrato inteiro. */
public record DisponivelParaGastarDto(BigDecimal valorDisponivel, BigDecimal saldoContas, BigDecimal faturasEmAberto,
                                       BigDecimal despesasRecorrentes, BigDecimal reserva) {
}
