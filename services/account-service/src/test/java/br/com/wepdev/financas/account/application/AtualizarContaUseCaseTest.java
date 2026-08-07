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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtualizarContaUseCaseTest {

    private final ContaRepository contaRepository = mock(ContaRepository.class);
    private final AtualizarContaUseCase useCase = new AtualizarContaUseCase(contaRepository);

    @Test
    void deveriaAtualizarESalvar_quandoUsuarioEDono() {
        UUID usuarioId = UUID.randomUUID();
        Conta conta = Conta.criar(usuarioId, "Conta corrente", TipoConta.CORRENTE, BigDecimal.TEN, "Banco X");
        when(contaRepository.buscarPorId(conta.getId())).thenReturn(Optional.of(conta));

        Conta resultado = useCase.executar(new AtualizarContaCommand(conta.getId(), usuarioId, "Novo nome", "Banco Y"));

        assertThat(resultado.getNome()).isEqualTo("Novo nome");
        assertThat(resultado.getInstituicao()).isEqualTo("Banco Y");
        verify(contaRepository).salvar(conta);
    }

    @Test
    void deveriaLancarExcecao_quandoContaNaoExiste() {
        UUID id = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(contaRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(new AtualizarContaCommand(id, usuarioId, "Nome", null)))
                .isInstanceOf(ContaNaoEncontradaException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoContaEhDeOutroUsuario() {
        UUID dono = UUID.randomUUID();
        UUID outroUsuario = UUID.randomUUID();
        Conta conta = Conta.criar(dono, "Conta corrente", TipoConta.CORRENTE, BigDecimal.TEN, "Banco X");
        when(contaRepository.buscarPorId(conta.getId())).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> useCase.executar(new AtualizarContaCommand(conta.getId(), outroUsuario, "Nome", null)))
                .isInstanceOf(ContaNaoEncontradaException.class);
    }
}
