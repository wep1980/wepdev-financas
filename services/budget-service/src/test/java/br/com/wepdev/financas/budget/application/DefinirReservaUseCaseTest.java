package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.Reserva;
import br.com.wepdev.financas.budget.domain.ReservaRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefinirReservaUseCaseTest {

    private final ReservaRepository reservaRepository = mock(ReservaRepository.class);
    private final DefinirReservaUseCase useCase = new DefinirReservaUseCase(reservaRepository);

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void deveriaCriarReserva_quandoUsuarioNuncaDefiniu() {
        when(reservaRepository.buscarPorUsuario(usuarioId)).thenReturn(Optional.empty());
        DefinirReservaCommand command = new DefinirReservaCommand(usuarioId, new BigDecimal("500.00"));

        Reserva reserva = useCase.executar(command);

        assertThat(reserva.getValor()).isEqualByComparingTo("500.00");
        verify(reservaRepository).salvar(reserva);
    }

    @Test
    void deveriaAtualizarReservaExistente() {
        Reserva existente = Reserva.definir(usuarioId, new BigDecimal("500.00"));
        when(reservaRepository.buscarPorUsuario(usuarioId)).thenReturn(Optional.of(existente));
        DefinirReservaCommand command = new DefinirReservaCommand(usuarioId, new BigDecimal("700.00"));

        Reserva reserva = useCase.executar(command);

        assertThat(reserva).isSameAs(existente);
        assertThat(reserva.getValor()).isEqualByComparingTo("700.00");
        verify(reservaRepository).salvar(existente);
    }
}
