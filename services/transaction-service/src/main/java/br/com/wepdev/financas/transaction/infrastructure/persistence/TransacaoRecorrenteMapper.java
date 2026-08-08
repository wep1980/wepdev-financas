package br.com.wepdev.financas.transaction.infrastructure.persistence;

import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;

final class TransacaoRecorrenteMapper {

    private TransacaoRecorrenteMapper() {
    }

    static TransacaoRecorrenteEntity paraNovaEntidade(TransacaoRecorrente regra) {
        TransacaoRecorrenteEntity entity = new TransacaoRecorrenteEntity();
        entity.id = regra.getId();
        atualizarEntidade(entity, regra);
        return entity;
    }

    static void atualizarEntidade(TransacaoRecorrenteEntity entity, TransacaoRecorrente regra) {
        entity.contaId = regra.getContaId();
        entity.usuarioId = regra.getUsuarioId();
        entity.descricao = regra.getDescricao();
        entity.valor = regra.getValor();
        entity.tipo = regra.getTipo();
        entity.categoria = regra.getCategoria();
        entity.frequencia = regra.getFrequencia();
        entity.dataInicio = regra.getDataInicio();
        entity.quantidadeOcorrencias = regra.getQuantidadeOcorrencias();
        entity.ocorrenciasGeradas = regra.getOcorrenciasGeradas();
        entity.status = regra.getStatus();
        entity.criadoEm = regra.getCriadoEm();
    }

    static TransacaoRecorrente paraDominio(TransacaoRecorrenteEntity entity) {
        return TransacaoRecorrente.reconstituir(
                entity.id,
                entity.contaId,
                entity.usuarioId,
                entity.descricao,
                entity.valor,
                entity.tipo,
                entity.categoria,
                entity.frequencia,
                entity.dataInicio,
                entity.quantidadeOcorrencias,
                entity.ocorrenciasGeradas,
                entity.status,
                entity.criadoEm
        );
    }
}
