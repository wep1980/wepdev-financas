package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.Reserva;
import br.com.wepdev.financas.budget.domain.ReservaRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuscarReservaUseCaseTest {

    private final ReservaRepository reservaRepository = mock(ReservaRepository.class);
    private final BuscarReservaUseCase useCase = new BuscarReservaUseCase(reservaRepository);

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void deveriaDevolverReservaExistente() {
        Reserva existente = Reserva.definir(usuarioId, new BigDecimal("500.00"));
        when(reservaRepository.buscarPorUsuario(usuarioId)).thenReturn(Optional.of(existente));

        Reserva reserva = useCase.executar(usuarioId);

        assertThat(reserva).isSameAs(existente);
    }

    @Test
    void deveriaDevolverReservaSemDefinir_quandoUsuarioNuncaConfigurou() {
        when(reservaRepository.buscarPorUsuario(usuarioId)).thenReturn(Optional.empty());

        Reserva reserva = useCase.executar(usuarioId);

        assertThat(reserva.getValor()).isEqualByComparingTo("0");
        assertThat(reserva.getAtualizadoEm()).isNull();
    }
}
