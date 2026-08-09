package br.com.wepdev.financas.card.infrastructure.persistence;

import br.com.wepdev.financas.card.domain.Parcela;

final class ParcelaMapper {

    private ParcelaMapper() {
    }

    static ParcelaEntity paraNovaEntidade(Parcela parcela) {
        ParcelaEntity entity = new ParcelaEntity();
        entity.id = parcela.getId();
        entity.faturaId = parcela.getFaturaId();
        entity.compraId = parcela.getCompraId();
        entity.descricao = parcela.getDescricao();
        entity.valor = parcela.getValor();
        entity.categoria = parcela.getCategoria();
        entity.numeroParcela = parcela.getNumeroParcela();
        entity.quantidadeParcelas = parcela.getQuantidadeParcelas();
        return entity;
    }

    static Parcela paraDominio(ParcelaEntity entity) {
        return Parcela.reconstituir(
                entity.id,
                entity.faturaId,
                entity.compraId,
                entity.descricao,
                entity.valor,
                entity.categoria,
                entity.numeroParcela,
                entity.quantidadeParcelas
        );
    }
}
