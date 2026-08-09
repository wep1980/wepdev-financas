package br.com.wepdev.financas.card.infrastructure.rest.dto;

import br.com.wepdev.financas.card.application.FaturaDetalhe;
import br.com.wepdev.financas.card.domain.StatusFatura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FaturaDetalheResponse(
        UUID id,
        UUID cartaoId,
        UUID usuarioId,
        String competencia,
        LocalDate dataFechamento,
        LocalDate dataVencimento,
        BigDecimal valorTotal,
        StatusFatura status,
        List<ParcelaDetalheResponse> parcelas
) {
    public static FaturaDetalheResponse de(FaturaDetalhe detalhe) {
        var fatura = detalhe.fatura();
        return new FaturaDetalheResponse(
                fatura.getId(),
                fatura.getCartaoId(),
                fatura.getUsuarioId(),
                fatura.getCompetencia().toString(),
                fatura.getDataFechamento(),
                fatura.getDataVencimento(),
                fatura.getValorTotal(),
                fatura.getStatus(),
                detalhe.parcelas().stream().map(ParcelaDetalheResponse::de).toList()
        );
    }
}
