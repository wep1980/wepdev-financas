package br.com.wepdev.financas.card.infrastructure.persistence;

import br.com.wepdev.financas.card.domain.Cartao;

final class CartaoMapper {

    private CartaoMapper() {
    }

    static CartaoEntity paraNovaEntidade(Cartao cartao) {
        CartaoEntity entity = new CartaoEntity();
        entity.id = cartao.getId();
        atualizarEntidade(entity, cartao);
        return entity;
    }

    static void atualizarEntidade(CartaoEntity entity, Cartao cartao) {
        entity.usuarioId = cartao.getUsuarioId();
        entity.apelido = cartao.getApelido();
        entity.bandeira = cartao.getBandeira();
        entity.limite = cartao.getLimite();
        entity.diaFechamento = cartao.getDiaFechamento();
        entity.diaVencimento = cartao.getDiaVencimento();
        entity.contaPagamentoId = cartao.getContaPagamentoId();
        entity.ativo = cartao.isAtivo();
        entity.criadoEm = cartao.getCriadoEm();
    }

    static Cartao paraDominio(CartaoEntity entity) {
        return Cartao.reconstituir(
                entity.id,
                entity.usuarioId,
                entity.apelido,
                entity.bandeira,
                entity.limite,
                entity.diaFechamento,
                entity.diaVencimento,
                entity.contaPagamentoId,
                entity.ativo,
                entity.criadoEm
        );
    }
}
