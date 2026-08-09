package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Bandeira;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import br.com.wepdev.financas.card.domain.StatusFatura;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarFaturasUseCaseTest {

    private final CartaoRepository cartaoRepository = mock(CartaoRepository.class);
    private final FaturaRepository faturaRepository = mock(FaturaRepository.class);
    private final ListarFaturasUseCase useCase = new ListarFaturasUseCase(cartaoRepository, faturaRepository);

    @Test
    void deveriaListarFaturasDoCartao() {
        UUID usuarioId = UUID.randomUUID();
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, UUID.randomUUID());
        Fatura fatura = Fatura.criar(cartao.getId(), usuarioId, YearMonth.of(2026, 8), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20));
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
        when(faturaRepository.listarPorCartao(cartao.getId(), null)).thenReturn(List.of(fatura));

        List<Fatura> resultado = useCase.executar(cartao.getId(), usuarioId, null);

        assertThat(resultado).containsExactly(fatura);
    }

    @Test
    void deveriaFiltrarPorStatus() {
        UUID usuarioId = UUID.randomUUID();
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, UUID.randomUUID());
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
        when(faturaRepository.listarPorCartao(cartao.getId(), StatusFatura.PAGA)).thenReturn(List.of());

        assertThat(useCase.executar(cartao.getId(), usuarioId, StatusFatura.PAGA)).isEmpty();
    }

    @Test
    void deveriaLancarExcecao_quandoCartaoNaoExiste() {
        UUID cartaoId = UUID.randomUUID();
        when(cartaoRepository.buscarPorId(cartaoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(cartaoId, UUID.randomUUID(), null))
                .isInstanceOf(CartaoNaoEncontradoException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoCartaoDeOutroUsuario() {
        Cartao cartao = Cartao.criar(UUID.randomUUID(), "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, UUID.randomUUID());
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

        assertThatThrownBy(() -> useCase.executar(cartao.getId(), UUID.randomUUID(), null))
                .isInstanceOf(CartaoNaoEncontradoException.class);
    }
}
