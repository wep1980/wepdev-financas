package br.com.wepdev.financas.card.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaturaTest {

    private final UUID cartaoId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final YearMonth competencia = YearMonth.of(2026, 8);

    @Test
    void deveriaCriarFaturaAbertaComValorTotalZero() {
        Fatura fatura = Fatura.criar(cartaoId, usuarioId, competencia, LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 12));

        assertThat(fatura.isAberta()).isTrue();
        assertThat(fatura.getValorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fatura.getId()).isNotNull();
    }

    @Test
    void deveriaAcumularValorTotal_aCadaParcelaAdicionada() {
        Fatura fatura = Fatura.criar(cartaoId, usuarioId, competencia, LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 12));

        fatura.adicionarParcela(new BigDecimal("100.00"));
        fatura.adicionarParcela(new BigDecimal("50.00"));

        assertThat(fatura.getValorTotal()).isEqualByComparingTo("150.00");
    }

    @Test
    void deveriaLancarExcecao_quandoAdicionarParcelaComValorZeroOuNegativo() {
        Fatura fatura = Fatura.criar(cartaoId, usuarioId, competencia, LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 12));

        assertThatThrownBy(() -> fatura.adicionarParcela(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> fatura.adicionarParcela(new BigDecimal("-1.00"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaFechar_eSerIdempotente() {
        Fatura fatura = Fatura.criar(cartaoId, usuarioId, competencia, LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 12));

        fatura.fechar();
        assertThat(fatura.isAberta()).isFalse();
        assertThat(fatura.getStatus()).isEqualTo(StatusFatura.FECHADA);

        fatura.fechar();
        assertThat(fatura.getStatus()).isEqualTo(StatusFatura.FECHADA);
    }

    @Test
    void naoDeveriaFechar_quandoJaPaga() {
        Fatura fatura = Fatura.criar(cartaoId, usuarioId, competencia, LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 12));
        fatura.fechar();
        fatura.pagar();

        fatura.fechar();

        assertThat(fatura.getStatus()).isEqualTo(StatusFatura.PAGA);
    }

    @Test
    void deveriaPagar_eSerIdempotente() {
        Fatura fatura = Fatura.criar(cartaoId, usuarioId, competencia, LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 12));
        fatura.fechar();

        fatura.pagar();
        assertThat(fatura.isPaga()).isTrue();

        fatura.pagar();
        assertThat(fatura.isPaga()).isTrue();
    }
}
