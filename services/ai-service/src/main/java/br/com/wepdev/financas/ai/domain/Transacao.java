package br.com.wepdev.financas.ai.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Leitura/escrita do transaction-service — tools MCP buscar_transacoes (leitura) e criar_transacao (escrita, PRD 3.5). */
public record Transacao(UUID id, UUID contaId, String descricao, BigDecimal valor, TipoTransacao tipo,
                         String categoria, LocalDate dataTransacao) {
}
