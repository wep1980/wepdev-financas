package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarTransacaoCommand(
        UUID contaId,
        UUID usuarioId,
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        String categoria,
        LocalDate dataTransacao
) {
}
