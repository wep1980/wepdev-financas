package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.AccountServiceClient;
import br.com.wepdev.financas.card.domain.Bandeira;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.ContaNaoEncontradaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtualizarCartaoUseCaseTest {

    private final CartaoRepository cartaoRepository = mock(CartaoRepository.class);
    private final AccountServiceClient accountServiceClient = mock(AccountServiceClient.class);
    private final AtualizarCartaoUseCase useCase = new AtualizarCartaoUseCase(cartaoRepository, accountServiceClient);

    @Test
    void deveriaAtualizarCartao() {
        UUID usuarioId = UUID.randomUUID();
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.MASTERCARD, new BigDecimal("5000.00"),
                5, 12, UUID.randomUUID());
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
        UUID novaConta = UUID.randomUUID();

        AtualizarCartaoCommand command = new AtualizarCartaoCommand(
                cartao.getId(), usuarioId, "Nubank renomeado", Bandeira.ELO, new BigDecimal("8000.00"), 10, 20, novaConta
        );

        Cartao atualizado = useCase.executar(command);

        assertThat(atualizado.getApelido()).isEqualTo("Nubank renomeado");
        assertThat(atualizado.getContaPagamentoId()).isEqualTo(novaConta);
        verify(accountServiceClient).confirmarPosseDaConta(novaConta);
        verify(cartaoRepository).salvar(cartao);
    }

    @Test
    void deveriaLancarExcecao_quandoNovaContaPagamentoNaoEncontrada() {
        UUID usuarioId = UUID.randomUUID();
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.MASTERCARD, new BigDecimal("5000.00"),
                5, 12, UUID.randomUUID());
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
        UUID novaConta = UUID.randomUUID();
        doThrow(new ContaNaoEncontradaException(novaConta)).when(accountServiceClient).confirmarPosseDaConta(novaConta);

        AtualizarCartaoCommand command = new AtualizarCartaoCommand(
                cartao.getId(), usuarioId, "Nubank", Bandeira.MASTERCARD, new BigDecimal("5000.00"), 5, 12, novaConta
        );

        assertThatThrownBy(() -> useCase.executar(command))
                .isInstanceOf(ContaNaoEncontradaException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoCartaoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(cartaoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        AtualizarCartaoCommand command = new AtualizarCartaoCommand(
                id, UUID.randomUUID(), "Nubank", Bandeira.MASTERCARD, new BigDecimal("5000.00"), 5, 12, UUID.randomUUID()
        );

        assertThatThrownBy(() -> useCase.executar(command))
                .isInstanceOf(CartaoNaoEncontradoException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoCartaoDeOutroUsuario() {
        UUID dono = UUID.randomUUID();
        Cartao cartao = Cartao.criar(dono, "Nubank", Bandeira.MASTERCARD, new BigDecimal("5000.00"),
                5, 12, UUID.randomUUID());
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

        AtualizarCartaoCommand command = new AtualizarCartaoCommand(
                cartao.getId(), UUID.randomUUID(), "Nubank", Bandeira.MASTERCARD, new BigDecimal("5000.00"), 5, 12, UUID.randomUUID()
        );

        assertThatThrownBy(() -> useCase.executar(command))
                .isInstanceOf(CartaoNaoEncontradoException.class);
    }
}
