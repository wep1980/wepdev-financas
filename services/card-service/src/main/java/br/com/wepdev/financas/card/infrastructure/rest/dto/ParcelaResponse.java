package br.com.wepdev.financas.card.infrastructure.rest.dto;

import br.com.wepdev.financas.card.application.ParcelaGerada;

import java.math.BigDecimal;
import java.util.UUID;

public record ParcelaResponse(
        UUID faturaId,
        String competencia,
        int numeroParcela,
        BigDecimal valor
) {
    public static ParcelaResponse de(ParcelaGerada parcela) {
        return new ParcelaResponse(parcela.faturaId(), parcela.competencia().toString(), parcela.numeroParcela(),
                parcela.valor());
    }
}
