package br.com.wepdev.financas.ai.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Payload de "transacao.eventos" — cópia própria do evento do transaction-service (sem lib compartilhada entre serviços, ADR-0001). */
public record TransacaoRegistradaEvento(
        UUID transacaoId,
        UUID contaId,
        UUID usuarioId,
        String descricao,
        String categoria,
        String tipo,
        BigDecimal valor,
        Instant criadoEm
) {
}
