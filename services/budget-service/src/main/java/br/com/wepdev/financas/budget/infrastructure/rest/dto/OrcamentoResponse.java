package br.com.wepdev.financas.budget.infrastructure.rest.dto;

import br.com.wepdev.financas.budget.application.OrcamentoDetalhe;
import br.com.wepdev.financas.budget.domain.Orcamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

public record OrcamentoResponse(
        UUID id,
        UUID usuarioId,
        String categoria,
        YearMonth mesReferencia,
        BigDecimal valorLimite,
        BigDecimal valorConsumido,
        BigDecimal valorDisponivel,
        BigDecimal percentualConsumido,
        String status,
        Instant criadoEm
) {
    public static OrcamentoResponse de(OrcamentoDetalhe detalhe) {
        Orcamento orcamento = detalhe.orcamento();
        BigDecimal valorConsumido = detalhe.valorConsumido();
        BigDecimal valorDisponivel = orcamento.getValorLimite().subtract(valorConsumido);
        BigDecimal percentualConsumido = valorConsumido
                .multiply(BigDecimal.valueOf(100))
                .divide(orcamento.getValorLimite(), 2, RoundingMode.HALF_UP);

        return new OrcamentoResponse(
                orcamento.getId(),
                orcamento.getUsuarioId(),
                orcamento.getCategoria(),
                orcamento.getMesReferencia(),
                orcamento.getValorLimite(),
                valorConsumido,
                valorDisponivel,
                percentualConsumido,
                orcamento.getStatus().name(),
                orcamento.getCriadoEm()
        );
    }
}
