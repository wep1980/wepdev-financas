package br.com.wepdev.financas.ai.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Porta de saída pro transaction-service (chamada síncrona), propagando
 * o token do próprio usuário. Cobre quatro tools MCP (ai-strategy.md
 * seção 4): buscar_transacoes (parte relacional — a semântica via
 * Qdrant entra no item 7), resumo_gastos_por_categoria, e
 * criar_transacao (a única tool de escrita do v1, PRD 3.5 — pontual ou
 * recorrente).
 */
public interface TransactionServiceClient {

    List<Transacao> buscarTransacoes(LocalDate inicio, LocalDate fim);

    List<ResumoCategoria> buscarResumoPorCategoria(LocalDate inicio, LocalDate fim);

    Transacao criarTransacao(CriarTransacaoComando comando);

    void criarTransacaoRecorrente(CriarTransacaoRecorrenteComando comando);
}
