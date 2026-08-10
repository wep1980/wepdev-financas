package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.Conta;
import br.com.wepdev.financas.budget.domain.DespesaRecorrente;
import br.com.wepdev.financas.budget.domain.FaturaFechada;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Totais + o detalhamento item a item de cada parcela — devolvido junto
 * de propósito, pra permitir auditoria (usuário) e explicação rastreável
 * (ai-service, PRD seção 6). Ver ADR-0026 pra fórmula e o porquê de cada
 * campo.
 */
public record DisponivelParaGastarResultado(
        YearMonth mesReferencia,
        BigDecimal saldoContas,
        BigDecimal faturasEmAberto,
        BigDecimal despesasRecorrentes,
        BigDecimal reserva,
        BigDecimal valorDisponivel,
        List<Conta> contas,
        List<FaturaFechada> faturas,
        List<DespesaRecorrente> despesasRecorrentesAtivas
) {
}
