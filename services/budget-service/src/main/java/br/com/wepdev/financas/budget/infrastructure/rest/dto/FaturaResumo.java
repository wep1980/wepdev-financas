package br.com.wepdev.financas.budget.infrastructure.rest.dto;

import br.com.wepdev.financas.budget.domain.FaturaFechada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FaturaResumo(UUID faturaId, String cartaoApelido, BigDecimal valorTotal, LocalDate dataVencimento) {
    static FaturaResumo de(FaturaFechada fatura) {
        return new FaturaResumo(fatura.faturaId(), fatura.cartaoApelido(), fatura.valorTotal(), fatura.dataVencimento());
    }
}
