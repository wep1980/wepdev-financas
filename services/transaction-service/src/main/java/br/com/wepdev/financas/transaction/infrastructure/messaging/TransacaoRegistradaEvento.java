package br.com.wepdev.financas.transaction.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Payload do evento publicado em "transacao.eventos" — mesmo formato JSON cru do ContaCriadaEvento (evolui pra CloudEvents/Avro depois). */
public record TransacaoRegistradaEvento(
        UUID transacaoId,
        UUID contaId,
        UUID usuarioId,
        String tipo,
        BigDecimal valor,
        Instant criadoEm
) {
}
