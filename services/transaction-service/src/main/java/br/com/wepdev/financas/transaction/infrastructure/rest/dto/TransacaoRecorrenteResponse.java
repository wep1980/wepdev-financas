package br.com.wepdev.financas.transaction.infrastructure.rest.dto;

import br.com.wepdev.financas.transaction.domain.FrequenciaRecorrencia;
import br.com.wepdev.financas.transaction.domain.StatusTransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransacaoRecorrenteResponse(
        UUID id,
        UUID contaId,
        UUID usuarioId,
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        String categoria,
        FrequenciaRecorrencia frequencia,
        LocalDate dataInicio,
        Integer quantidadeOcorrencias,
        int ocorrenciasGeradas,
        StatusTransacaoRecorrente status,
        Instant criadoEm
) {
    public static TransacaoRecorrenteResponse de(TransacaoRecorrente regra) {
        return new TransacaoRecorrenteResponse(
                regra.getId(),
                regra.getContaId(),
                regra.getUsuarioId(),
                regra.getDescricao(),
                regra.getValor(),
                regra.getTipo(),
                regra.getCategoria(),
                regra.getFrequencia(),
                regra.getDataInicio(),
                regra.getQuantidadeOcorrencias(),
                regra.getOcorrenciasGeradas(),
                regra.getStatus(),
                regra.getCriadoEm()
        );
    }
}
