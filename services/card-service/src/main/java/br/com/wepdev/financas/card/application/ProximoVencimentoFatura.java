package br.com.wepdev.financas.card.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProximoVencimentoFatura(
        UUID faturaId,
        UUID cartaoId,
        UUID usuarioId,
        String apelidoCartao,
        BigDecimal valorTotal,
        LocalDate dataVencimento
) {
}
