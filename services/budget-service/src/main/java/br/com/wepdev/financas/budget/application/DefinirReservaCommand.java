package br.com.wepdev.financas.budget.application;

import java.math.BigDecimal;
import java.util.UUID;

public record DefinirReservaCommand(UUID usuarioId, BigDecimal valor) {
}
