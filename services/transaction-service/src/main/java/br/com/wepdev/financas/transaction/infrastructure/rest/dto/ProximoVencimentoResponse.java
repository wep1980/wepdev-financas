package br.com.wepdev.financas.transaction.infrastructure.rest.dto;

import br.com.wepdev.financas.transaction.application.ProximoVencimento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProximoVencimentoResponse(
        UUID transacaoRecorrenteId,
        UUID usuarioId,
        String descricao,
        BigDecimal valor,
        LocalDate dataVencimentoPrevista
) {
    public static ProximoVencimentoResponse de(ProximoVencimento vencimento) {
        return new ProximoVencimentoResponse(
                vencimento.transacaoRecorrenteId(),
                vencimento.usuarioId(),
                vencimento.descricao(),
                vencimento.valor(),
                vencimento.dataVencimentoPrevista()
        );
    }
}
