package br.com.wepdev.financas.document.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LancamentoConfirmadoPayload(
        UUID lancamentoId,
        String descricao,
        BigDecimal valor,
        String tipo,
        String categoria,
        LocalDate data
) {
}
