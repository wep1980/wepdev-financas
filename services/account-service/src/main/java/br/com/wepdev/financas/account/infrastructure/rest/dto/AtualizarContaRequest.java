package br.com.wepdev.financas.account.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record AtualizarContaRequest(
        @NotBlank String nome,
        String instituicao
) {
}
