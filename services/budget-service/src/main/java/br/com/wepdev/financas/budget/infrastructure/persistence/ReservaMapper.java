package br.com.wepdev.financas.budget.infrastructure.persistence;

import br.com.wepdev.financas.budget.domain.Reserva;

final class ReservaMapper {

    private ReservaMapper() {
    }

    static ReservaEntity paraNovaEntidade(Reserva reserva) {
        ReservaEntity entity = new ReservaEntity();
        entity.usuarioId = reserva.getUsuarioId();
        atualizarEntidade(entity, reserva);
        return entity;
    }

    static void atualizarEntidade(ReservaEntity entity, Reserva reserva) {
        entity.valor = reserva.getValor();
        entity.atualizadoEm = reserva.getAtualizadoEm();
    }

    static Reserva paraDominio(ReservaEntity entity) {
        return Reserva.reconstituir(entity.usuarioId, entity.valor, entity.atualizadoEm);
    }
}
