package br.com.wepdev.financas.transaction.application;

import java.util.List;
import java.util.UUID;

public record ProcessarLancamentosConfirmadosCommand(
        UUID usuarioId,
        UUID contaId,
        List<LancamentoConfirmadoCommand> lancamentos
) {
}
