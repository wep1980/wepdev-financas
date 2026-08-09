package br.com.wepdev.financas.card.infrastructure.rest.dto;

import br.com.wepdev.financas.card.domain.Bandeira;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/** usuarioId não vem aqui — é extraído do token (sub), nunca confiado do cliente (ADR-0003). */
public record CriarCartaoRequest(
        @NotBlank String apelido,
        Bandeira bandeira,
        @NotNull @Positive BigDecimal limite,
        @Min(1) @Max(31) int diaFechamento,
        @Min(1) @Max(31) int diaVencimento,
        @NotNull UUID contaPagamentoId
) {
}
