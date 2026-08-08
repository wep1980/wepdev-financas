package br.com.wepdev.financas.transaction.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AtualizarTransacaoCommand(
        UUID id,
        UUID usuarioId,
        String descricao,
        BigDecimal valor,
        String categoria,
        LocalDate dataTransacao
) {
}
