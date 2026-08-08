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
        LocalDate dataTransacao,
        UUID transacaoRecorrenteId
) {
    /** Registro comum (não vindo de uma regra recorrente) — transacaoRecorrenteId fica null. */
    public RegistrarTransacaoCommand(UUID contaId, UUID usuarioId, String descricao, BigDecimal valor,
                                      TipoTransacao tipo, String categoria, LocalDate dataTransacao) {
        this(contaId, usuarioId, descricao, valor, tipo, categoria, dataTransacao, null);
    }
}
