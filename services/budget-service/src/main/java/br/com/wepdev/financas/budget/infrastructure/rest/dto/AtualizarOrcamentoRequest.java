package br.com.wepdev.financas.budget.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AtualizarOrcamentoRequest(@NotNull @Positive BigDecimal valorLimite) {
}
