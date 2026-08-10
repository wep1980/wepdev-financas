package br.com.wepdev.financas.ai.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/** Leitura do card-service — dado que a tool buscar_fatura_cartao devolve pro agente. */
public record Fatura(UUID id, YearMonth competencia, LocalDate dataVencimento, BigDecimal valorTotal, StatusFatura status) {
}
