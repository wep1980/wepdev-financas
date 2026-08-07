package br.com.wepdev.financas.account.infrastructure.rest.dto;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.TipoConta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContaResponse(
        UUID id,
        UUID usuarioId,
        String nome,
        TipoConta tipo,
        BigDecimal saldo,
        String instituicao,
        boolean ativa,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static ContaResponse de(Conta conta) {
        return new ContaResponse(
                conta.getId(),
                conta.getUsuarioId(),
                conta.getNome(),
                conta.getTipo(),
                conta.getSaldo(),
                conta.getInstituicao(),
                conta.isAtiva(),
                conta.getCriadoEm(),
                conta.getAtualizadoEm()
        );
    }
}
