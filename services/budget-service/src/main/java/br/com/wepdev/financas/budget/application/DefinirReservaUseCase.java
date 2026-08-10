package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.Reserva;
import br.com.wepdev.financas.budget.domain.ReservaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DefinirReservaUseCase {

    private final ReservaRepository reservaRepository;

    public DefinirReservaUseCase(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    /** Upsert — primeira vez cria, chamadas seguintes atualizam a mesma linha (ADR-0026: valor único, sem histórico). */
    @Transactional
    public Reserva executar(DefinirReservaCommand command) {
        Reserva reserva = reservaRepository.buscarPorUsuario(command.usuarioId())
                .orElse(null);

        if (reserva == null) {
            reserva = Reserva.definir(command.usuarioId(), command.valor());
        } else {
            reserva.atualizar(command.valor());
        }

        reservaRepository.salvar(reserva);
        return reserva;
    }
}
