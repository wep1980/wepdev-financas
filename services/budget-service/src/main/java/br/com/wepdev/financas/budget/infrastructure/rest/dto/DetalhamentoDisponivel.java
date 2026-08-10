package br.com.wepdev.financas.budget.infrastructure.rest.dto;

import br.com.wepdev.financas.budget.application.DisponivelParaGastarResultado;

import java.util.List;

/** Item a item de cada parcela do cálculo, pra auditoria/explicação (ADR-0026). */
public record DetalhamentoDisponivel(
        List<ContaResumo> contas,
        List<FaturaResumo> faturas,
        List<DespesaRecorrenteResumo> despesasRecorrentes
) {
    static DetalhamentoDisponivel de(DisponivelParaGastarResultado resultado) {
        return new DetalhamentoDisponivel(
                resultado.contas().stream().map(ContaResumo::de).toList(),
                resultado.faturas().stream().map(FaturaResumo::de).toList(),
                resultado.despesasRecorrentesAtivas().stream().map(DespesaRecorrenteResumo::de).toList()
        );
    }
}
