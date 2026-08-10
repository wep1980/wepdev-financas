package br.com.wepdev.financas.document.infrastructure.persistence;

import br.com.wepdev.financas.document.domain.LancamentoPendente;

final class LancamentoPendenteMapper {

    private LancamentoPendenteMapper() {
    }

    static LancamentoPendenteEntity paraNovaEntidade(LancamentoPendente lancamento) {
        LancamentoPendenteEntity entity = new LancamentoPendenteEntity();
        entity.id = lancamento.getId();
        atualizarEntidade(entity, lancamento);
        return entity;
    }

    static void atualizarEntidade(LancamentoPendenteEntity entity, LancamentoPendente lancamento) {
        entity.documentoId = lancamento.getDocumentoId();
        entity.descricao = lancamento.getDescricao();
        entity.valor = lancamento.getValor();
        entity.data = lancamento.getData();
        entity.tipo = lancamento.getTipo();
        entity.categoriaSugerida = lancamento.getCategoriaSugerida();
        entity.status = lancamento.getStatus();
    }

    static LancamentoPendente paraDominio(LancamentoPendenteEntity entity) {
        return LancamentoPendente.reconstituir(
                entity.id,
                entity.documentoId,
                entity.descricao,
                entity.valor,
                entity.data,
                entity.tipo,
                entity.categoriaSugerida,
                entity.status
        );
    }
}
