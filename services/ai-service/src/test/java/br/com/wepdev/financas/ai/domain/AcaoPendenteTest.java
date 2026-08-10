package br.com.wepdev.financas.ai.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcaoPendenteTest {

    @Test
    void deveriaProporAcaoPontual_comExpiracaoDezMinutosNoFuturo() {
        Instant antes = Instant.now();

        AcaoPendente acao = AcaoPendente.propor(TipoTransacao.DESPESA, "Mercado", new BigDecimal("100.00"), false,
                null, null, null, "Mercado");

        assertThat(acao.getTipo()).isEqualTo(TipoTransacao.DESPESA);
        assertThat(acao.getDescricao()).isEqualTo("Mercado");
        assertThat(acao.getValor()).isEqualByComparingTo("100.00");
        assertThat(acao.isRecorrente()).isFalse();
        assertThat(acao.getCategoria()).isEqualTo("Mercado");
        assertThat(acao.getExpiraEm()).isAfter(antes.plus(9, ChronoUnit.MINUTES));
        assertThat(acao.getExpiraEm()).isBefore(antes.plus(11, ChronoUnit.MINUTES));
    }

    @Test
    void deveriaProporAcaoRecorrente_comFrequencia() {
        AcaoPendente acao = AcaoPendente.propor(TipoTransacao.RECEITA, "Salário", new BigDecimal("10000.00"), true,
                FrequenciaRecorrencia.MENSAL, null, UUID.randomUUID(), "Salário");

        assertThat(acao.isRecorrente()).isTrue();
        assertThat(acao.getFrequencia()).isEqualTo(FrequenciaRecorrencia.MENSAL);
        assertThat(acao.getQuantidadeOcorrencias()).isNull();
    }

    @Test
    void deveriaLancarExcecao_quandoRecorrenteSemFrequencia() {
        assertThatThrownBy(() -> AcaoPendente.propor(TipoTransacao.DESPESA, "Aluguel", new BigDecimal("100.00"), true,
                null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoDescricaoVazia() {
        assertThatThrownBy(() -> AcaoPendente.propor(TipoTransacao.DESPESA, null, new BigDecimal("100.00"), false,
                null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AcaoPendente.propor(TipoTransacao.DESPESA, "  ", new BigDecimal("100.00"), false,
                null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoValorZeroOuNegativo() {
        assertThatThrownBy(() -> AcaoPendente.propor(TipoTransacao.DESPESA, "Mercado", BigDecimal.ZERO, false,
                null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AcaoPendente.propor(TipoTransacao.DESPESA, "Mercado", new BigDecimal("-1"), false,
                null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isExpirada_deveriaSerFalse_antesDoPrazo() {
        AcaoPendente acao = AcaoPendente.propor(TipoTransacao.DESPESA, "Mercado", new BigDecimal("100.00"), false,
                null, null, null, null);

        assertThat(acao.isExpirada(Instant.now())).isFalse();
    }

    @Test
    void isExpirada_deveriaSerTrue_depoisDoPrazo() {
        AcaoPendente acao = AcaoPendente.propor(TipoTransacao.DESPESA, "Mercado", new BigDecimal("100.00"), false,
                null, null, null, null);

        assertThat(acao.isExpirada(Instant.now().plus(11, ChronoUnit.MINUTES))).isTrue();
    }
}
