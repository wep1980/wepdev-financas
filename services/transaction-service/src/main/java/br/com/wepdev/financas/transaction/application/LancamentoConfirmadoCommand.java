package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoConfirmadoCommand(
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        String categoria,
        LocalDate data
) {
}
