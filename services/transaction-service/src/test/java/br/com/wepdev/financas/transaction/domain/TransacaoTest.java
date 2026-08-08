package br.com.wepdev.financas.transaction.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransacaoTest {

    private final UUID contaId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void deveriaCriarTransacaoConfirmada_quandoDadosValidos() {
        Transacao transacao = Transacao.criar(contaId, usuarioId, "Salário", new BigDecimal("5000.00"),
                TipoTransacao.RECEITA, "Salário", LocalDate.of(2026, 8, 5));

        assertThat(transacao.getId()).isNotNull();
        assertThat(transacao.getStatus()).isEqualTo(StatusTransacao.CONFIRMADA);
        assertThat(transacao.getTransacaoRecorrenteId()).isNull();
        assertThat(transacao.getDataTransacao()).isEqualTo(LocalDate.of(2026, 8, 5));
    }

    @Test
    void deveriaUsarDataAtual_quandoDataTransacaoNaoInformada() {
        Transacao transacao = Transacao.criar(contaId, usuarioId, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", null);

        assertThat(transacao.getDataTransacao()).isEqualTo(LocalDate.now());
    }

    @Test
    void deveriaLancarExcecao_quandoValorZeroOuNegativo() {
        assertThatThrownBy(() -> Transacao.criar(contaId, usuarioId, "Mercado", BigDecimal.ZERO,
                TipoTransacao.DESPESA, "Alimentação", null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Transacao.criar(contaId, usuarioId, "Mercado", new BigDecimal("-10.00"),
                TipoTransacao.DESPESA, "Alimentação", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoContaIdNulo() {
        assertThatThrownBy(() -> Transacao.criar(null, usuarioId, "Mercado", BigDecimal.TEN,
                TipoTransacao.DESPESA, "Alimentação", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveriaCancelarTransacao() {
        Transacao transacao = Transacao.criar(contaId, usuarioId, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", null);

        transacao.cancelar();

        assertThat(transacao.getStatus()).isEqualTo(StatusTransacao.CANCELADA);
        assertThat(transacao.isCancelada()).isTrue();
    }

    @Test
    void deveriaAtualizarCamposEditaveis() {
        Transacao transacao = Transacao.criar(contaId, usuarioId, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", LocalDate.of(2026, 8, 1));

        transacao.atualizar("Mercado (ajustado)", new BigDecimal("150.00"), "Casa", LocalDate.of(2026, 8, 2));

        assertThat(transacao.getDescricao()).isEqualTo("Mercado (ajustado)");
        assertThat(transacao.getValor()).isEqualByComparingTo("150.00");
        assertThat(transacao.getCategoria()).isEqualTo("Casa");
        assertThat(transacao.getDataTransacao()).isEqualTo(LocalDate.of(2026, 8, 2));
    }

    @Test
    void deveriaManterDataAtual_quandoAtualizarSemInformarNovaData() {
        Transacao transacao = Transacao.criar(contaId, usuarioId, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", LocalDate.of(2026, 8, 1));

        transacao.atualizar("Mercado", new BigDecimal("100.00"), "Alimentação", null);

        assertThat(transacao.getDataTransacao()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    void deveriaLancarExcecao_quandoAtualizarComValorZeroOuNegativo() {
        Transacao transacao = Transacao.criar(contaId, usuarioId, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", null);

        assertThatThrownBy(() -> transacao.atualizar("Mercado", BigDecimal.ZERO, "Alimentação", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
