package br.com.wepdev.financas.ai.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Parâmetros da tool de escrita criar_transacao (PRD 3.5) pra uma transação pontual — já confirmada pelo usuário (ADR-0007) quando isso é chamado. */
public record CriarTransacaoComando(UUID contaId, String descricao, BigDecimal valor, TipoTransacao tipo,
                                     String categoria, LocalDate dataTransacao) {
}
