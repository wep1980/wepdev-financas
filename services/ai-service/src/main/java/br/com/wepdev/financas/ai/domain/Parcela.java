package br.com.wepdev.financas.ai.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Leitura do card-service — detalhe de uma parcela dentro de uma Fatura específica (tool valor_fatura_mes). */
public record Parcela(UUID compraId, String descricao, String categoria, int numeroParcela, int quantidadeParcelas,
                       BigDecimal valor) {
}
