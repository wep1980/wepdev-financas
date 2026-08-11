package br.com.wepdev.financas.document.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Espelha LancarCompraRequest do card-service (docs/specs/card-service.yaml). */
public record LancarCompraRequestDto(
        String descricao,
        BigDecimal valorTotal,
        String categoria,
        LocalDate dataCompra,
        Integer quantidadeParcelas
) {
}
