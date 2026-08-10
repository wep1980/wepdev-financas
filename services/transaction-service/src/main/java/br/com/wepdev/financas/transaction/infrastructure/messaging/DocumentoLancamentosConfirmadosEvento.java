package br.com.wepdev.financas.transaction.infrastructure.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload consumido do tópico "documento.lancamentos-confirmados"
 * (publicado pelo document-service, overview.md seção 3, ADR-0023).
 * Posse de {@code contaId} já foi confirmada no document-service antes de
 * publicar — este serviço não reverifica (ADR-0025).
 */
public record DocumentoLancamentosConfirmadosEvento(
        UUID documentoId,
        UUID usuarioId,
        UUID contaId,
        List<LancamentoConfirmadoPayload> lancamentos,
        Instant confirmadoEm
) {
}
