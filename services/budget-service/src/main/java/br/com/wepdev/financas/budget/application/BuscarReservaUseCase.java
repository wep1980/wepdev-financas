package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.Reserva;
import br.com.wepdev.financas.budget.domain.ReservaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class BuscarReservaUseCase {

    private final ReservaRepository reservaRepository;

    public BuscarReservaUseCase(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    /** Nunca 404 — usuário que nunca definiu reserva recebe Reserva.semDefinir (valor 0). */
    public Reserva executar(UUID usuarioId) {
        return reservaRepository.buscarPorUsuario(usuarioId).orElseGet(() -> Reserva.semDefinir(usuarioId));
    }
}
