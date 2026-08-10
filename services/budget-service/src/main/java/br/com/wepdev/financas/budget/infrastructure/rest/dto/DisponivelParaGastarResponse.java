package br.com.wepdev.financas.budget.infrastructure.rest.dto;

import br.com.wepdev.financas.budget.application.DisponivelParaGastarResultado;

import java.math.BigDecimal;
import java.time.YearMonth;

public record DisponivelParaGastarResponse(
        YearMonth mesReferencia,
        BigDecimal saldoContas,
        BigDecimal faturasEmAberto,
        BigDecimal despesasRecorrentes,
        BigDecimal reserva,
        BigDecimal valorDisponivel,
        DetalhamentoDisponivel detalhamento
) {
    public static DisponivelParaGastarResponse de(DisponivelParaGastarResultado resultado) {
        return new DisponivelParaGastarResponse(
                resultado.mesReferencia(),
                resultado.saldoContas(),
                resultado.faturasEmAberto(),
                resultado.despesasRecorrentes(),
                resultado.reserva(),
                resultado.valorDisponivel(),
                DetalhamentoDisponivel.de(resultado)
        );
    }
}
