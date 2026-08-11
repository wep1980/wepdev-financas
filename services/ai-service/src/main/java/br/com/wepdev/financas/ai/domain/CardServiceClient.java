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

    /** Parcelas de uma fatura específica — usado pra separar parcelado vs à vista (tool valor_fatura_mes, 2026-08-11). */
    List<Parcela> buscarParcelasDaFatura(UUID faturaId);

    /** Compras agrupadas por compraId, ainda ativas ou não (tool compras_parceladas, 2026-08-11). */
    List<CompraResumo> listarCompras(UUID cartaoId);
}
