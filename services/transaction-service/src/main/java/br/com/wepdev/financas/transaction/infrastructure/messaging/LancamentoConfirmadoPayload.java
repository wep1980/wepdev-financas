package br.com.wepdev.financas.transaction.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Espelha LancamentoConfirmadoPayload do document-service — mesmo formato JSON, serviços diferentes não compartilham código. */
public record LancamentoConfirmadoPayload(
        UUID lancamentoId,
        String descricao,
        BigDecimal valor,
        String tipo,
        String categoria,
        LocalDate data
) {
}
