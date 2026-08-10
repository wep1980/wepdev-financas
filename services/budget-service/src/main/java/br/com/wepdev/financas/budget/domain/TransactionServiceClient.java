package br.com.wepdev.financas.budget.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Porta de saída pro transaction-service (chamada síncrona, ADR-0026),
 * propagando o token do próprio usuário — sem confirmação de posse, os
 * endpoints já filtram pelo `sub` do token.
 */
public interface TransactionServiceClient {

    /**
     * Regras de despesa recorrente ATIVA do usuário ("contas fixas" do
     * PRD 3.3) — aproximadas como 1 compromisso fixo por mês (ver
     * ADR-0026). Filtrar por dataInicio dentro/antes do mês consultado é
     * responsabilidade de quem chama.
     */
    List<DespesaRecorrente> buscarDespesasRecorrentesAtivas();

    /** Mesmo cálculo usado pelo dashboard/IA (PRD 3.7) — usado aqui pra Orcamento.valorConsumido. */
    List<ResumoCategoria> buscarResumoPorCategoria(LocalDate inicio, LocalDate fim);
}
