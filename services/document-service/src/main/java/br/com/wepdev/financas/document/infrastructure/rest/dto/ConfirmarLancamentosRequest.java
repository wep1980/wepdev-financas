package br.com.wepdev.financas.document.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record ConfirmarLancamentosRequest(
        @NotNull UUID contaId,
        @NotNull Set<UUID> lancamentoIdsConfirmados
) {
}
