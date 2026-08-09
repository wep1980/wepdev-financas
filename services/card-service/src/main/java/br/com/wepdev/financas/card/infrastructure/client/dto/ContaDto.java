package br.com.wepdev.financas.card.infrastructure.client.dto;

import java.util.UUID;

/** Só os campos que este serviço precisa da resposta do account-service — não é o contrato inteiro. */
public record ContaDto(UUID id, UUID usuarioId) {
}
