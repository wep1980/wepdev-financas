package br.com.wepdev.financas.document.infrastructure.client.dto;

import br.com.wepdev.financas.document.domain.CompraExistente;

import java.math.BigDecimal;

/** Espelha CompraResumoResponse do card-service (docs/specs/card-service.yaml). */
public record CompraResumoDto(
        String descricao,
        String categoria,
        BigDecimal valorParcela,
        int quantidadeParcelas,
        int parcelasRestantes,
        BigDecimal valorTotalRestante,
        boolean finalizada
) {
    public CompraExistente paraDominio() {
        return new CompraExistente(descricao, valorParcela);
    }
}
