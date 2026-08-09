package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Bandeira;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuscarCartaoUseCaseTest {

    private final CartaoRepository cartaoRepository = mock(CartaoRepository.class);
    private final BuscarCartaoUseCase useCase = new BuscarCartaoUseCase(cartaoRepository);

    @Test
    void deveriaRetornarCartao_quandoDonoConfere() {
        UUID usuarioId = UUID.randomUUID();
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.MASTERCARD, new BigDecimal("5000.00"),
                5, 12, UUID.randomUUID());
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

        assertThat(useCase.executar(cartao.getId(), usuarioId)).isEqualTo(cartao);
    }

    @Test
    void deveriaLancarExcecao_quandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(cartaoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, UUID.randomUUID()))
                .isInstanceOf(CartaoNaoEncontradoException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoDeOutroUsuario() {
        UUID dono = UUID.randomUUID();
        Cartao cartao = Cartao.criar(dono, "Nubank", Bandeira.MASTERCARD, new BigDecimal("5000.00"),
                5, 12, UUID.randomUUID());
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

        assertThatThrownBy(() -> useCase.executar(cartao.getId(), UUID.randomUUID()))
                .isInstanceOf(CartaoNaoEncontradoException.class);
    }
}
