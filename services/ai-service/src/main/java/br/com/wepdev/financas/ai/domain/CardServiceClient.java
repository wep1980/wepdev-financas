package br.com.wepdev.financas.ai.domain;

import java.util.List;
import java.util.UUID;

/**
 * Porta de saída pro card-service (chamada síncrona, tool MCP
 * buscar_fatura_cartao — ai-strategy.md seção 4), propagando o token do
 * próprio usuário.
 */
public interface CardServiceClient {

    /** Todos os cartões ativos do usuário — o agente resolve "cartão X" por apelido a partir disso (item 8). */
    List<Cartao> buscarCartoesAtivos();

    List<Fatura> buscarFaturas(UUID cartaoId, StatusFatura statusFiltro);
}
