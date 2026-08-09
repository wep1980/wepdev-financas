package br.com.wepdev.financas.card.infrastructure.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancarCompraRequest(
        @NotBlank String descricao,
        @NotNull @Positive BigDecimal valorTotal,
        String categoria,
        @NotNull LocalDate dataCompra,
        @Min(1) Integer quantidadeParcelas
) {
    /** 1 = à vista, mesmo default documentado na spec quando o campo é omitido. */
    public int quantidadeParcelasOuUm() {
        return quantidadeParcelas == null ? 1 : quantidadeParcelas;
    }
}
