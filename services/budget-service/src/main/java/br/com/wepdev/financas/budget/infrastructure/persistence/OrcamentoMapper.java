package br.com.wepdev.financas.budget.infrastructure.persistence;

import br.com.wepdev.financas.budget.domain.Orcamento;

import java.time.YearMonth;

final class OrcamentoMapper {

    private OrcamentoMapper() {
    }

    static OrcamentoEntity paraNovaEntidade(Orcamento orcamento) {
        OrcamentoEntity entity = new OrcamentoEntity();
        entity.id = orcamento.getId();
        atualizarEntidade(entity, orcamento);
        return entity;
    }

    static void atualizarEntidade(OrcamentoEntity entity, Orcamento orcamento) {
        entity.usuarioId = orcamento.getUsuarioId();
        entity.categoria = orcamento.getCategoria();
        entity.mesReferencia = orcamento.getMesReferencia().toString();
        entity.valorLimite = orcamento.getValorLimite();
        entity.status = orcamento.getStatus();
        entity.criadoEm = orcamento.getCriadoEm();
    }

    static Orcamento paraDominio(OrcamentoEntity entity) {
        return Orcamento.reconstituir(
                entity.id,
                entity.usuarioId,
                entity.categoria,
                YearMonth.parse(entity.mesReferencia),
                entity.valorLimite,
                entity.status,
                entity.criadoEm
        );
    }
}
