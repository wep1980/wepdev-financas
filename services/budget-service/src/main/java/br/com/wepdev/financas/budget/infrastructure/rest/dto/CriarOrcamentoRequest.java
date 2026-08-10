package br.com.wepdev.financas.budget.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.YearMonth;

/** usuarioId não vem aqui — é extraído do token (sub), nunca confiado do cliente (ADR-0003). */
public record CriarOrcamentoRequest(
        @NotBlank String categoria,
        @NotNull YearMonth mesReferencia,
        @NotNull @Positive BigDecimal valorLimite
) {
}
