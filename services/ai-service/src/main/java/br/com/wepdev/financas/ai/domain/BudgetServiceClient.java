package br.com.wepdev.financas.ai.domain;

import java.time.YearMonth;

/**
 * Porta de saída pro budget-service (chamada síncrona, tool MCP
 * buscar_saldo_disponivel — ai-strategy.md seção 4), propagando o token
 * do próprio usuário — sem confirmação de posse, o endpoint já filtra
 * pelo `sub` do token (mesmo padrão do budget-service consultando os
 * outros serviços, ADR-0026).
 */
public interface BudgetServiceClient {

    DisponivelParaGastar buscarDisponivelParaGastar(YearMonth mes);
}
