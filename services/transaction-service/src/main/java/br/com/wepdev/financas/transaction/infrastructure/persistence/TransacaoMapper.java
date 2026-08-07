package br.com.wepdev.financas.transaction.infrastructure.persistence;

import br.com.wepdev.financas.transaction.domain.Transacao;

final class TransacaoMapper {

    private TransacaoMapper() {
    }

    static TransacaoEntity paraNovaEntidade(Transacao transacao) {
        TransacaoEntity entity = new TransacaoEntity();
        entity.id = transacao.getId();
        atualizarEntidade(entity, transacao);
        return entity;
    }

    static void atualizarEntidade(TransacaoEntity entity, Transacao transacao) {
        entity.contaId = transacao.getContaId();
        entity.usuarioId = transacao.getUsuarioId();
        entity.descricao = transacao.getDescricao();
        entity.valor = transacao.getValor();
        entity.tipo = transacao.getTipo();
        entity.categoria = transacao.getCategoria();
        entity.dataTransacao = transacao.getDataTransacao();
        entity.status = transacao.getStatus();
        entity.transacaoRecorrenteId = transacao.getTransacaoRecorrenteId();
        entity.criadoEm = transacao.getCriadoEm();
    }

    static Transacao paraDominio(TransacaoEntity entity) {
        return Transacao.reconstituir(
                entity.id,
                entity.contaId,
                entity.usuarioId,
                entity.descricao,
                entity.valor,
                entity.tipo,
                entity.categoria,
                entity.dataTransacao,
                entity.status,
                entity.transacaoRecorrenteId,
                entity.criadoEm
        );
    }
}
