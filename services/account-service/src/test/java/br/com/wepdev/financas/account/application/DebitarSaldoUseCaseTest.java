package br.com.wepdev.financas.account.application;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.ContaNaoEncontradaException;
import br.com.wepdev.financas.account.domain.ContaRepository;
import br.com.wepdev.financas.account.domain.SaldoInsuficienteException;
import br.com.wepdev.financas.account.domain.TipoConta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DebitarSaldoUseCaseTest {

    private final ContaRepository contaRepository = mock(ContaRepository.class);
    private final DebitarSaldoUseCase useCase = new DebitarSaldoUseCase(contaRepository);

    @Test
    void deveriaDebitarEDeSalvar_quandoSaldoSuficiente() {
        Conta conta = Conta.criar(UUID.randomUUID(), "Conta corrente", TipoConta.CORRENTE, new BigDecimal("100.00"), "Banco X");
        when(contaRepository.buscarPorId(conta.getId())).thenReturn(Optional.of(conta));

        Conta resultado = useCase.executar(conta.getId(), new BigDecimal("30.00"));

        assertThat(resultado.getSaldo()).isEqualByComparingTo("70.00");
        verify(contaRepository).salvar(conta);
    }

    @Test
    void deveriaLancarExcecao_quandoSaldoInsuficiente() {
        Conta conta = Conta.criar(UUID.randomUUID(), "Conta corrente", TipoConta.CORRENTE, new BigDecimal("10.00"), "Banco X");
        when(contaRepository.buscarPorId(conta.getId())).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> useCase.executar(conta.getId(), new BigDecimal("30.00")))
                .isInstanceOf(SaldoInsuficienteException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoContaNaoExiste() {
        UUID id = UUID.randomUUID();
        when(contaRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, BigDecimal.TEN))
                .isInstanceOf(ContaNaoEncontradaException.class);
    }
}
