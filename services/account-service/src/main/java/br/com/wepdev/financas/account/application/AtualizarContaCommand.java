package br.com.wepdev.financas.account.application;

import java.util.UUID;

public record AtualizarContaCommand(
        UUID id,
        UUID usuarioId,
        String nome,
        String instituicao
) {
}
