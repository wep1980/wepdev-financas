package br.com.wepdev.financas.account.application;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.ContaNaoEncontradaException;
import br.com.wepdev.financas.account.domain.ContaRepository;
import br.com.wepdev.financas.account.domain.TipoConta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuscarContaUseCaseTest {

    private final ContaRepository contaRepository = mock(ContaRepository.class);
    private final BuscarContaUseCase useCase = new BuscarContaUseCase(contaRepository);

    @Test
    void deveriaRetornarConta_quandoExisteEUsuarioEDono() {
        UUID usuarioId = UUID.randomUUID();
        Conta conta = Conta.criar(usuarioId, "Conta corrente", TipoConta.CORRENTE, BigDecimal.TEN, "Banco X");
        when(contaRepository.buscarPorId(conta.getId())).thenReturn(Optional.of(conta));

        Conta resultado = useCase.executar(conta.getId(), usuarioId);

        assertThat(resultado).isEqualTo(conta);
    }

    @Test
    void deveriaLancarExcecao_quandoContaNaoExiste() {
        UUID id = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(contaRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, usuarioId))
                .isInstanceOf(ContaNaoEncontradaException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoContaEhDeOutroUsuario() {
        UUID dono = UUID.randomUUID();
        UUID outroUsuario = UUID.randomUUID();
        Conta conta = Conta.criar(dono, "Conta corrente", TipoConta.CORRENTE, BigDecimal.TEN, "Banco X");
        when(contaRepository.buscarPorId(conta.getId())).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> useCase.executar(conta.getId(), outroUsuario))
                .isInstanceOf(ContaNaoEncontradaException.class);
    }
}
