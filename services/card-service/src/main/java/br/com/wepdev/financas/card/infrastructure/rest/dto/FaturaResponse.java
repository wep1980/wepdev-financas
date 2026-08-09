package br.com.wepdev.financas.card.infrastructure.rest.dto;

import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.StatusFatura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FaturaResponse(
        UUID id,
        UUID cartaoId,
        UUID usuarioId,
        String competencia,
        LocalDate dataFechamento,
        LocalDate dataVencimento,
        BigDecimal valorTotal,
        StatusFatura status
) {
    public static FaturaResponse de(Fatura fatura) {
        return new FaturaResponse(
                fatura.getId(),
                fatura.getCartaoId(),
                fatura.getUsuarioId(),
                fatura.getCompetencia().toString(),
                fatura.getDataFechamento(),
                fatura.getDataVencimento(),
                fatura.getValorTotal(),
                fatura.getStatus()
        );
    }
}
