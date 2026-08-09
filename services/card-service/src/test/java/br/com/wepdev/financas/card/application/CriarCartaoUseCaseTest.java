package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.AccountServiceClient;
import br.com.wepdev.financas.card.domain.Bandeira;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.ContaNaoEncontradaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CriarCartaoUseCaseTest {

    private final CartaoRepository cartaoRepository = mock(CartaoRepository.class);
    private final AccountServiceClient accountServiceClient = mock(AccountServiceClient.class);
    private final CriarCartaoUseCase useCase = new CriarCartaoUseCase(cartaoRepository, accountServiceClient);

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID contaPagamentoId = UUID.randomUUID();

    @Test
    void deveriaCriarESalvarCartao_quandoContaPagamentoValida() {
        CriarCartaoCommand command = new CriarCartaoCommand(
                usuarioId, "Nubank", Bandeira.MASTERCARD, new BigDecimal("5000.00"), 5, 12, contaPagamentoId
        );

        Cartao cartao = useCase.executar(command);

        assertThat(cartao.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(cartao.getContaPagamentoId()).isEqualTo(contaPagamentoId);
        verify(accountServiceClient).confirmarPosseDaConta(contaPagamentoId);
        verify(cartaoRepository).salvar(cartao);
    }

    @Test
    void naoDeveriaSalvar_quandoContaPagamentoNaoEncontrada() {
        doThrow(new ContaNaoEncontradaException(contaPagamentoId))
                .when(accountServiceClient).confirmarPosseDaConta(contaPagamentoId);
        CriarCartaoCommand command = new CriarCartaoCommand(
                usuarioId, "Nubank", Bandeira.MASTERCARD, new BigDecimal("5000.00"), 5, 12, contaPagamentoId
        );

        assertThatThrownBy(() -> useCase.executar(command))
                .isInstanceOf(ContaNaoEncontradaException.class);
        verify(cartaoRepository, never()).salvar(any());
    }
}
