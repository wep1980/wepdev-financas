package br.com.wepdev.financas.ai.infrastructure.client.dto;

import java.util.UUID;

/** Só os campos que este serviço precisa da resposta do card-service — não é o contrato inteiro. */
public record CartaoDto(UUID id, String apelido) {
}
