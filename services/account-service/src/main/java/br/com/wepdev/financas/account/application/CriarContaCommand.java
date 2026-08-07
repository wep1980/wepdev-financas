package br.com.wepdev.financas.account.application;

import br.com.wepdev.financas.account.domain.TipoConta;

import java.math.BigDecimal;
import java.util.UUID;

public record CriarContaCommand(
        UUID usuarioId,
        String nome,
        TipoConta tipo,
        BigDecimal saldoInicial,
        String instituicao
) {
}
