package br.com.wepdev.financas.card.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LancarCompraCommand(
        UUID cartaoId,
        UUID usuarioId,
        String descricao,
        BigDecimal valorTotal,
        String categoria,
        LocalDate dataCompra,
        int quantidadeParcelas
) {
}
