package br.com.wepdev.financas.card.infrastructure.rest.dto;

import br.com.wepdev.financas.card.application.CompraResultado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CompraResponse(
        UUID id,
        UUID cartaoId,
        String descricao,
        BigDecimal valorTotal,
        String categoria,
        LocalDate dataCompra,
        int quantidadeParcelas,
        List<ParcelaResponse> parcelas
) {
    public static CompraResponse de(CompraResultado resultado) {
        return new CompraResponse(
                resultado.compraId(),
                resultado.cartaoId(),
                resultado.descricao(),
                resultado.valorTotal(),
                resultado.categoria(),
                resultado.dataCompra(),
                resultado.quantidadeParcelas(),
                resultado.parcelas().stream().map(ParcelaResponse::de).toList()
        );
    }
}
