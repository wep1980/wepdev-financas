package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.FrequenciaRecorrencia;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CriarTransacaoRecorrenteCommand(
        UUID contaId,
        UUID usuarioId,
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        String categoria,
        FrequenciaRecorrencia frequencia,
        LocalDate dataInicio,
        Integer quantidadeOcorrencias
) {
}
