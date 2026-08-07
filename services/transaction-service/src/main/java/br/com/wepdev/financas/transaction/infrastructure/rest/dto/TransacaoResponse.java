package br.com.wepdev.financas.transaction.infrastructure.rest.dto;

import br.com.wepdev.financas.transaction.domain.StatusTransacao;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import br.com.wepdev.financas.transaction.domain.Transacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransacaoResponse(
        UUID id,
        UUID contaId,
        UUID usuarioId,
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        String categoria,
        LocalDate dataTransacao,
        StatusTransacao status,
        UUID transacaoRecorrenteId,
        Instant criadoEm
) {
    public static TransacaoResponse de(Transacao transacao) {
        return new TransacaoResponse(
                transacao.getId(),
                transacao.getContaId(),
                transacao.getUsuarioId(),
                transacao.getDescricao(),
                transacao.getValor(),
                transacao.getTipo(),
                transacao.getCategoria(),
                transacao.getDataTransacao(),
                transacao.getStatus(),
                transacao.getTransacaoRecorrenteId(),
                transacao.getCriadoEm()
        );
    }
}
