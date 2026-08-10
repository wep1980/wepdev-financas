package br.com.wepdev.financas.budget.infrastructure.rest.dto;

import br.com.wepdev.financas.budget.domain.Reserva;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReservaResponse(UUID usuarioId, BigDecimal valor, Instant atualizadoEm) {
    public static ReservaResponse de(Reserva reserva) {
        return new ReservaResponse(reserva.getUsuarioId(), reserva.getValor(), reserva.getAtualizadoEm());
    }
}
