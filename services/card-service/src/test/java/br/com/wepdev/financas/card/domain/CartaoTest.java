package br.com.wepdev.financas.card.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartaoTest {

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID contaPagamentoId = UUID.randomUUID();

    @Test
    void deveriaCriarCartaoAtivo_quandoDadosValidos() {
        Cartao cartao = Cartao.criar(usuarioId, "Nubank Roxinho", Bandeira.MASTERCARD, new BigDecimal("5000.00"),
                5, 12, contaPagamentoId);

        assertThat(cartao.isAtivo()).isTrue();
        assertThat(cartao.getId()).isNotNull();
        assertThat(cartao.getApelido()).isEqualTo("Nubank Roxinho");
        assertThat(cartao.getDiaFechamento()).isEqualTo(5);
        assertThat(cartao.getDiaVencimento()).isEqualTo(12);
    }

    @Test
    void deveriaCriarCartaoSemBandeira_quandoBandeiraNula() {
        Cartao cartao = Cartao.criar(usuarioId, "Cartão genérico", null, new BigDecimal("1000.00"), 1, 10, contaPagamentoId);

        assertThat(cartao.getBandeira()).isNull();
    }

    @Test
    void deveriaLancarExcecao_quandoLimiteZeroOuNegativo() {
        assertThatThrownBy(() -> Cartao.criar(usuarioId, "Cartão", Bandeira.VISA, BigDecimal.ZERO, 5, 12, contaPagamentoId))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Cartao.criar(usuarioId, "Cartão", Bandeira.VISA, new BigDecimal("-1"), 5, 12, contaPagamentoId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoApelidoVazio() {
        assertThatThrownBy(() -> Cartao.criar(usuarioId, "  ", Bandeira.VISA, new BigDecimal("1000.00"), 5, 12, contaPagamentoId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoDiaForaDoIntervalo() {
        assertThatThrownBy(() -> Cartao.criar(usuarioId, "Cartão", Bandeira.VISA, new BigDecimal("1000.00"), 0, 12, contaPagamentoId))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Cartao.criar(usuarioId, "Cartão", Bandeira.VISA, new BigDecimal("1000.00"), 5, 32, contaPagamentoId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaAtualizarCamposEditaveis() {
        Cartao cartao = Cartao.criar(usuarioId, "Cartão antigo", Bandeira.VISA, new BigDecimal("1000.00"), 5, 12, contaPagamentoId);
        UUID novaContaPagamento = UUID.randomUUID();

        cartao.atualizar("Cartão novo", Bandeira.ELO, new BigDecimal("2000.00"), 10, 20, novaContaPagamento);

        assertThat(cartao.getApelido()).isEqualTo("Cartão novo");
        assertThat(cartao.getBandeira()).isEqualTo(Bandeira.ELO);
        assertThat(cartao.getLimite()).isEqualByComparingTo("2000.00");
        assertThat(cartao.getDiaFechamento()).isEqualTo(10);
        assertThat(cartao.getDiaVencimento()).isEqualTo(20);
        assertThat(cartao.getContaPagamentoId()).isEqualTo(novaContaPagamento);
    }

    @Test
    void deveriaInativarCartao_eSerIdempotente() {
        Cartao cartao = Cartao.criar(usuarioId, "Cartão", Bandeira.VISA, new BigDecimal("1000.00"), 5, 12, contaPagamentoId);

        cartao.inativar();
        assertThat(cartao.isAtivo()).isFalse();

        cartao.inativar();
        assertThat(cartao.isAtivo()).isFalse();
    }
}
