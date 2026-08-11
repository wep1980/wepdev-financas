package br.com.wepdev.financas.ai.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Só os campos que este serviço precisa da resposta do card-service — não é o contrato inteiro. */
public record CompraResumoDto(UUID compraId, String descricao, String categoria, BigDecimal valorParcela,
                               int quantidadeParcelas, int parcelasRestantes, BigDecimal valorTotalRestante,
                               boolean finalizada) {
}
