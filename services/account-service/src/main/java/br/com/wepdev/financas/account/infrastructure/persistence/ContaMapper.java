package br.com.wepdev.financas.account.infrastructure.persistence;

import br.com.wepdev.financas.account.domain.Conta;

final class ContaMapper {

    private ContaMapper() {
    }

    static ContaEntity paraNovaEntidade(Conta conta) {
        ContaEntity entity = new ContaEntity();
        atualizarEntidade(entity, conta);
        entity.id = conta.getId();
        return entity;
    }

    static void atualizarEntidade(ContaEntity entity, Conta conta) {
        entity.usuarioId = conta.getUsuarioId();
        entity.nome = conta.getNome();
        entity.tipo = conta.getTipo();
        entity.saldo = conta.getSaldo();
        entity.instituicao = conta.getInstituicao();
        entity.ativa = conta.isAtiva();
        entity.criadoEm = conta.getCriadoEm();
        entity.atualizadoEm = conta.getAtualizadoEm();
    }

    static Conta paraDominio(ContaEntity entity) {
        return Conta.reconstituir(
                entity.id,
                entity.usuarioId,
                entity.nome,
                entity.tipo,
                entity.saldo,
                entity.instituicao,
                entity.ativa,
                entity.criadoEm,
                entity.atualizadoEm
        );
    }
}
