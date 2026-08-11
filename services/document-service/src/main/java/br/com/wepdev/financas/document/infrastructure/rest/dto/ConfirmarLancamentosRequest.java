package br.com.wepdev.financas.document.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

/** Sem contaId (ADR-0028) — quem paga a fatura é o contaPagamentoId do próprio Cartao, definido no card-service. */
public record ConfirmarLancamentosRequest(
        @NotNull Set<UUID> lancamentoIdsConfirmados
) {
}
