package br.com.wepdev.financas.transaction.infrastructure.rest.dto;

import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** usuarioId não vem aqui — é extraído do token (sub), nunca confiado do cliente (ADR-0003). */
public record CriarTransacaoRequest(
        @NotNull UUID contaId,
        @NotBlank String descricao,
        @NotNull @Positive BigDecimal valor,
        @NotNull TipoTransacao tipo,
        String categoria,
        LocalDate dataTransacao
) {
}
