package br.com.wepdev.financas.ai.domain;

/** As tools MCP de leitura (ai-strategy.md seção 4) — criar_transacao (escrita) não é uma "consulta", tratada à parte no fluxo de ação. */
public enum ToolConsulta {
    SALDO_DISPONIVEL,
    RESUMO_CATEGORIA,
    FATURA_CARTAO,
    TRANSACOES,
    /** Compras parceladas ativas — quantas, maior parcela, quanto falta de cada uma (2026-08-11). */
    COMPRAS_PARCELADAS,
    /** Valor da fatura de um mês específico, separado em parcelado vs à vista (2026-08-11). */
    VALOR_FATURA_MES,
    /** Categoria com mais gasto num período — combina transaction-service e card-service (2026-08-11). */
    CATEGORIA_MAIS_GASTOU,
    /** "No que você consegue me ajudar?" — resposta fixa, sem chamada externa (2026-08-11). */
    CAPACIDADES
}
