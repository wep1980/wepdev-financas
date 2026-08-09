package br.com.wepdev.financas.card.infrastructure.rest.dto;

import br.com.wepdev.financas.card.application.ProximoVencimentoFatura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProximoVencimentoFaturaResponse(
        UUID faturaId,
        UUID cartaoId,
        UUID usuarioId,
        String apelidoCartao,
        BigDecimal valorTotal,
        LocalDate dataVencimento
) {
    public static ProximoVencimentoFaturaResponse de(ProximoVencimentoFatura vencimento) {
        return new ProximoVencimentoFaturaResponse(
                vencimento.faturaId(),
                vencimento.cartaoId(),
                vencimento.usuarioId(),
                vencimento.apelidoCartao(),
                vencimento.valorTotal(),
                vencimento.dataVencimento()
        );
    }
}
