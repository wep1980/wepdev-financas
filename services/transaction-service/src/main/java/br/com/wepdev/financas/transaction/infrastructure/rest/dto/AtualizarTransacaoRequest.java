package br.com.wepdev.financas.transaction.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/** contaId e tipo não são editáveis aqui de propósito — ver docs/specs/transaction-service.yaml. */
public record AtualizarTransacaoRequest(
        @NotBlank String descricao,
        @NotNull @Positive BigDecimal valor,
        String categoria,
        LocalDate dataTransacao
) {
}
