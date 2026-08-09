package br.com.wepdev.financas.card.application;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record ParcelaGerada(
        UUID faturaId,
        YearMonth competencia,
        int numeroParcela,
        BigDecimal valor
) {
}
