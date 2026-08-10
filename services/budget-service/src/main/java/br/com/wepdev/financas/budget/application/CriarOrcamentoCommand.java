package br.com.wepdev.financas.budget.application;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record CriarOrcamentoCommand(UUID usuarioId, String categoria, YearMonth mesReferencia, BigDecimal valorLimite) {
}
