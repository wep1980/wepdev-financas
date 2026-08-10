package br.com.wepdev.financas.transaction.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payload do evento publicado em "transacao.eventos" — mesmo formato JSON
 * cru do ContaCriadaEvento (evolui pra CloudEvents/Avro depois).
 * {@code descricao}/{@code categoria} adicionados quando o ai-service
 * passou a consumir esse tópico pra indexar embedding no Qdrant (RAG,
 * ai-strategy.md seção 2 — "descrição de transações" é justamente o que
 * vira embedding); mudança aditiva, sem consumidor anterior pra quebrar.
 */
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
