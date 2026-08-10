package br.com.wepdev.financas.budget.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Leitura do transaction-service (ADR-0026) — regra de despesa recorrente ATIVA do usuário. */
public record DespesaRecorrente(UUID transacaoRecorrenteId, String descricao, BigDecimal valor, LocalDate dataInicio) {
}
