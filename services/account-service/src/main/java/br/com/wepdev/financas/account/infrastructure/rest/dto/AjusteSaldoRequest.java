package br.com.wepdev.financas.account.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AjusteSaldoRequest(
        @NotNull @Positive BigDecimal valor
) {
}
