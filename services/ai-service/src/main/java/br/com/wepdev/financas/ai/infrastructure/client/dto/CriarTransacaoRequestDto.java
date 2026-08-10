package br.com.wepdev.financas.ai.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Corpo de POST /api/v1/transacoes do transaction-service — espelha docs/specs/transaction-service.yaml. */
public record CriarTransacaoRequestDto(UUID contaId, String descricao, BigDecimal valor, String tipo,
                                        String categoria, LocalDate dataTransacao) {
}
