package br.com.wepdev.financas.budget.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record DefinirReservaRequest(@NotNull @PositiveOrZero BigDecimal valor) {
}
