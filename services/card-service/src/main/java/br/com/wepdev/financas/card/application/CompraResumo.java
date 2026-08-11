package br.com.wepdev.financas.card.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Uma compra vista pelo agrupamento de suas {@link br.com.wepdev.financas.card.domain.Parcela}s
 * (mesmo {@code compraId}) — não existe uma tabela "compras" (ver
 * Javadoc de {@code Parcela}). {@code valorParcela}/{@code categoria} vêm
 * da primeira parcela (numeroParcela=1); as demais têm o mesmo valor,
 * exceto a última, que absorve o arredondamento (ver
 * {@code LancarCompraUseCase.dividir}).
 * {@code parcelasRestantes}/{@code valorTotalRestante} contam só parcelas
 * cuja fatura ainda está ABERTA (ainda não cobrada) — ver
 * {@link ListarComprasUseCase}.
 */
public record CompraResumo(
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
}
