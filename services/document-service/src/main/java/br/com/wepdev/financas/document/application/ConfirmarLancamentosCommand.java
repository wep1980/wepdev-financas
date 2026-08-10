package br.com.wepdev.financas.document.application;

import java.util.Set;
import java.util.UUID;

public record ConfirmarLancamentosCommand(
        UUID documentoId,
        UUID usuarioId,
        UUID contaId,
        Set<UUID> lancamentoIdsConfirmados
) {
}
