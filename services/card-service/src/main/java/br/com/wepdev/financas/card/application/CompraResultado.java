package br.com.wepdev.financas.card.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CompraResultado(
        UUID compraId,
        UUID cartaoId,
        String descricao,
        BigDecimal valorTotal,
        String categoria,
        LocalDate dataCompra,
        int quantidadeParcelas,
        List<ParcelaGerada> parcelas
) {
}
