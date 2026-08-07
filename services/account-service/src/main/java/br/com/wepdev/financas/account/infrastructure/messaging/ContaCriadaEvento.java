package br.com.wepdev.financas.account.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payload do evento publicado em "conta.eventos" — ainda JSON cru (ver
 * docs/architecture/overview.md seção 7); evolui pra CloudEvents/Avro depois,
 * não é bloqueante agora.
 */
public record ContaCriadaEvento(
        UUID contaId,
        UUID usuarioId,
        String tipo,
        BigDecimal saldoInicial,
        Instant criadoEm
) {
}
