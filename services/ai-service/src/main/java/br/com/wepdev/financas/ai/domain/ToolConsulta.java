package br.com.wepdev.financas.ai.domain;

/** As quatro tools MCP de leitura (ai-strategy.md seção 4) — criar_transacao (escrita) não é uma "consulta", tratada à parte no fluxo de ação. */
public enum ToolConsulta {
    SALDO_DISPONIVEL,
    RESUMO_CATEGORIA,
    FATURA_CARTAO,
    TRANSACOES
}
