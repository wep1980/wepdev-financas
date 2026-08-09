package br.com.wepdev.financas.card.infrastructure.rest.dto;

import br.com.wepdev.financas.card.domain.Parcela;

import java.math.BigDecimal;
import java.util.UUID;

public record ParcelaDetalheResponse(
        UUID compraId,
        String descricao,
        String categoria,
        int numeroParcela,
        int quantidadeParcelas,
        BigDecimal valor
) {
    public static ParcelaDetalheResponse de(Parcela parcela) {
        return new ParcelaDetalheResponse(
                parcela.getCompraId(),
                parcela.getDescricao(),
                parcela.getCategoria(),
                parcela.getNumeroParcela(),
                parcela.getQuantidadeParcelas(),
                parcela.getValor()
        );
    }
}
