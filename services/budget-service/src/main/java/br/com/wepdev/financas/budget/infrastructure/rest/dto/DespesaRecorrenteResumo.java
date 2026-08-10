package br.com.wepdev.financas.budget.infrastructure.rest.dto;

import br.com.wepdev.financas.budget.domain.DespesaRecorrente;

import java.math.BigDecimal;
import java.util.UUID;

public record DespesaRecorrenteResumo(UUID transacaoRecorrenteId, String descricao, BigDecimal valor) {
    static DespesaRecorrenteResumo de(DespesaRecorrente despesa) {
        return new DespesaRecorrenteResumo(despesa.transacaoRecorrenteId(), despesa.descricao(), despesa.valor());
    }
}
