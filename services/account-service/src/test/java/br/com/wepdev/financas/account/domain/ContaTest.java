package br.com.wepdev.financas.account.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContaTest {

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void deveriaCriarContaComSaldoZero_quandoSaldoInicialNaoInformado() {
        Conta conta = Conta.criar(usuarioId, "Conta corrente", TipoConta.CORRENTE, null, "Banco X");

        assertThat(conta.getSaldo()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(conta.isAtiva()).isTrue();
        assertThat(conta.getId()).isNotNull();
    }

    @Test
    void deveriaLancarExcecao_quandoSaldoInicialNegativo() {
        assertThatThrownBy(() ->
                Conta.criar(usuarioId, "Conta corrente", TipoConta.CORRENTE, new BigDecimal("-10.00"), "Banco X")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaDebitarSaldo_quandoValorMenorOuIgualAoSaldo() {
        Conta conta = Conta.criar(usuarioId, "Carteira", TipoConta.CARTEIRA, new BigDecimal("100.00"), null);

        conta.debitar(new BigDecimal("30.00"));

        assertThat(conta.getSaldo()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    void deveriaLancarSaldoInsuficiente_quandoDebitoMaiorQueSaldo() {
        Conta conta = Conta.criar(usuarioId, "Carteira", TipoConta.CARTEIRA, new BigDecimal("50.00"), null);

        assertThatThrownBy(() -> conta.debitar(new BigDecimal("50.01")))
                .isInstanceOf(SaldoInsuficienteException.class);
        assertThat(conta.getSaldo()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void deveriaCreditarSaldo_quandoValorPositivo() {
        Conta conta = Conta.criar(usuarioId, "Carteira", TipoConta.CARTEIRA, BigDecimal.ZERO, null);

        conta.creditar(new BigDecimal("25.50"));

        assertThat(conta.getSaldo()).isEqualByComparingTo(new BigDecimal("25.50"));
    }

    @Test
    void deveriaLancarExcecao_quandoDebitarValorZeroOuNegativo() {
        Conta conta = Conta.criar(usuarioId, "Carteira", TipoConta.CARTEIRA, new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> conta.debitar(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> conta.debitar(new BigDecimal("-5.00"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaInativarConta() {
        Conta conta = Conta.criar(usuarioId, "Carteira", TipoConta.CARTEIRA, BigDecimal.ZERO, null);

        conta.inativar();

        assertThat(conta.isAtiva()).isFalse();
    }

    @Test
    void deveriaAtualizarNomeEInstituicao() {
        Conta conta = Conta.criar(usuarioId, "Carteira", TipoConta.CARTEIRA, BigDecimal.ZERO, "Banco X");

        conta.atualizar("Carteira renomeada", "Banco Y");

        assertThat(conta.getNome()).isEqualTo("Carteira renomeada");
        assertThat(conta.getInstituicao()).isEqualTo("Banco Y");
    }

    @Test
    void deveriaLancarExcecao_quandoAtualizarComNomeNulo() {
        Conta conta = Conta.criar(usuarioId, "Carteira", TipoConta.CARTEIRA, BigDecimal.ZERO, "Banco X");

        assertThatThrownBy(() -> conta.atualizar(null, "Banco Y"))
                .isInstanceOf(NullPointerException.class);
    }
}
