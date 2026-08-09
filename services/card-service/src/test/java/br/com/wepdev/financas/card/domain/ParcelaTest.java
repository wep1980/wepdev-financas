package br.com.wepdev.financas.card.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParcelaTest {

    private final UUID faturaId = UUID.randomUUID();
    private final UUID compraId = UUID.randomUUID();

    @Test
    void deveriaCriarParcela_quandoDadosValidos() {
        Parcela parcela = Parcela.criar(faturaId, compraId, "Mercado", new BigDecimal("50.00"), "Alimentação", 1, 3);

        assertThat(parcela.getId()).isNotNull();
        assertThat(parcela.getNumeroParcela()).isEqualTo(1);
        assertThat(parcela.getQuantidadeParcelas()).isEqualTo(3);
    }

    @Test
    void deveriaLancarExcecao_quandoValorZeroOuNegativo() {
        assertThatThrownBy(() -> Parcela.criar(faturaId, compraId, "Mercado", BigDecimal.ZERO, "Alimentação", 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoNumeroParcelaForaDoIntervalo() {
        assertThatThrownBy(() -> Parcela.criar(faturaId, compraId, "Mercado", new BigDecimal("10.00"), "Alimentação", 0, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Parcela.criar(faturaId, compraId, "Mercado", new BigDecimal("10.00"), "Alimentação", 4, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
