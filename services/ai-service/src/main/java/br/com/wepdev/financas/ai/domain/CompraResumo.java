package br.com.wepdev.financas.ai.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Leitura do card-service — compra agrupada por compraId (tool compras_parceladas). Ver ListarComprasUseCase no card-service. */
public record CompraResumo(UUID compraId, String descricao, String categoria, BigDecimal valorParcela,
                            int quantidadeParcelas, int parcelasRestantes, BigDecimal valorTotalRestante,
                            boolean finalizada) {
}
