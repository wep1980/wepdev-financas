package br.com.wepdev.financas.card.infrastructure.rest.dto;

import br.com.wepdev.financas.card.application.CompraResumo;

import java.math.BigDecimal;
import java.util.UUID;

public record CompraResumoResponse(
        UUID compraId,
        UUID cartaoId,
        String descricao,
        String categoria,
        BigDecimal valorParcela,
        int quantidadeParcelas,
        int parcelasRestantes,
        BigDecimal valorTotalRestante,
        boolean finalizada
) {
    public static CompraResumoResponse de(CompraResumo resumo) {
        return new CompraResumoResponse(
                resumo.compraId(),
                resumo.cartaoId(),
                resumo.descricao(),
                resumo.categoria(),
                resumo.valorParcela(),
                resumo.quantidadeParcelas(),
                resumo.parcelasRestantes(),
                resumo.valorTotalRestante(),
                resumo.finalizada()
        );
    }
}
