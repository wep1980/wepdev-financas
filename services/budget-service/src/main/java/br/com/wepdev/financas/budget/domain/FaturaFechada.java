package br.com.wepdev.financas.budget.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Leitura do card-service (ADR-0026) — fatura FECHADA (valor definitivo, ainda não paga) de algum cartão do usuário. */
public record FaturaFechada(UUID faturaId, String cartaoApelido, BigDecimal valorTotal, LocalDate dataVencimento) {
}
