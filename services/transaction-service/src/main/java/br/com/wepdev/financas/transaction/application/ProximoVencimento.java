package br.com.wepdev.financas.transaction.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProximoVencimento(
        UUID transacaoRecorrenteId,
        UUID usuarioId,
        String descricao,
        BigDecimal valor,
        LocalDate dataVencimentoPrevista
) {
}
