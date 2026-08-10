package br.com.wepdev.financas.budget.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservaTest {

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void deveriaDefinirReserva_quandoValorValido() {
        Reserva reserva = Reserva.definir(usuarioId, new BigDecimal("500.00"));

        assertThat(reserva.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(reserva.getValor()).isEqualByComparingTo("500.00");
        assertThat(reserva.getAtualizadoEm()).isNotNull();
    }

    @Test
    void deveriaPermitirValorZero() {
        Reserva reserva = Reserva.definir(usuarioId, BigDecimal.ZERO);

        assertThat(reserva.getValor()).isEqualByComparingTo("0");
    }

    @Test
    void deveriaLancarExcecao_quandoValorNegativo() {
        assertThatThrownBy(() -> Reserva.definir(usuarioId, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaCriarReservaSemDefinir_comValorZeroEAtualizadoEmNulo() {
        Reserva reserva = Reserva.semDefinir(usuarioId);

        assertThat(reserva.getValor()).isEqualByComparingTo("0");
        assertThat(reserva.getAtualizadoEm()).isNull();
    }

    @Test
    void deveriaAtualizarValor() {
        Reserva reserva = Reserva.definir(usuarioId, new BigDecimal("500.00"));

        reserva.atualizar(new BigDecimal("700.00"));

        assertThat(reserva.getValor()).isEqualByComparingTo("700.00");
    }
}
