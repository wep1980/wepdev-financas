package br.com.wepdev.financas.document.infrastructure.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Payload do evento publicado em "documento.lancamentos-confirmados" — consumido pelo transaction-service (overview.md seção 3, ADR-0023). */
public record DocumentoLancamentosConfirmadosEvento(
        UUID documentoId,
        UUID usuarioId,
        UUID contaId,
        List<LancamentoConfirmadoPayload> lancamentos,
        Instant confirmadoEm
) {
}
