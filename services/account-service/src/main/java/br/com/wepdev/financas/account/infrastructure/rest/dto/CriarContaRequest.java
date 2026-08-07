package br.com.wepdev.financas.account.infrastructure.rest.dto;

import br.com.wepdev.financas.account.domain.TipoConta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** usuarioId não vem aqui — é extraído do token (sub), nunca confiado do cliente (ADR-0003). */
public record CriarContaRequest(
        @NotBlank String nome,
        @NotNull TipoConta tipo,
        @PositiveOrZero BigDecimal saldoInicial,
        String instituicao
) {
}
