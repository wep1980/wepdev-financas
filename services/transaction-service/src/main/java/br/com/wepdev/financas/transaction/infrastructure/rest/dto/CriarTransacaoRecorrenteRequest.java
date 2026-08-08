package br.com.wepdev.financas.transaction.infrastructure.rest.dto;

import br.com.wepdev.financas.transaction.domain.FrequenciaRecorrencia;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CriarTransacaoRecorrenteRequest(
        @NotNull UUID contaId,
        @NotBlank String descricao,
        @NotNull @Positive BigDecimal valor,
        @NotNull TipoTransacao tipo,
        String categoria,
        @NotNull FrequenciaRecorrencia frequencia,
        @NotNull LocalDate dataInicio,
        Integer quantidadeOcorrencias
) {
}
