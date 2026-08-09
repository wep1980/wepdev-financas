package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.AccountServiceClient;
import br.com.wepdev.financas.card.domain.Bandeira;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaAindaAbertaException;
import br.com.wepdev.financas.card.domain.FaturaNaoEncontradaException;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import br.com.wepdev.financas.card.domain.SaldoInsuficienteException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PagarFaturaUseCaseTest {

    private final FaturaRepository faturaRepository = mock(FaturaRepository.class);
    private final CartaoRepository cartaoRepository = mock(CartaoRepository.class);
    private final AccountServiceClient accountServiceClient = mock(AccountServiceClient.class);
    private final PagarFaturaUseCase useCase = new PagarFaturaUseCase(faturaRepository, cartaoRepository, accountServiceClient);

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID contaPagamentoId = UUID.randomUUID();

    @Test
    void deveriaPagarFatura_quandoFechada() {
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, contaPagamentoId);
        Fatura fatura = Fatura.criar(cartao.getId(), usuarioId, YearMonth.of(2026, 8), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20));
        fatura.adicionarParcela(new BigDecimal("150.00"));
        fatura.fechar();
        when(faturaRepository.buscarPorId(fatura.getId())).thenReturn(Optional.of(fatura));
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

        useCase.executar(fatura.getId(), usuarioId);

        verify(accountServiceClient).debitar(contaPagamentoId, new BigDecimal("150.00"));
        assertThat(fatura.isPaga()).isTrue();
        verify(faturaRepository).salvar(fatura);
    }

    @Test
    void naoDeveriaDebitarDeNovo_quandoJaPaga() {
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, contaPagamentoId);
        Fatura fatura = Fatura.criar(cartao.getId(), usuarioId, YearMonth.of(2026, 8), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20));
        fatura.adicionarParcela(new BigDecimal("150.00"));
        fatura.fechar();
        fatura.pagar();
        when(faturaRepository.buscarPorId(fatura.getId())).thenReturn(Optional.of(fatura));

        useCase.executar(fatura.getId(), usuarioId);

        verify(accountServiceClient, never()).debitar(any(), any());
        verify(faturaRepository, never()).salvar(any());
    }

    @Test
    void deveriaLancarExcecao_quandoFaturaAindaAberta() {
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, contaPagamentoId);
        Fatura fatura = Fatura.criar(cartao.getId(), usuarioId, YearMonth.of(2026, 8), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20));
        when(faturaRepository.buscarPorId(fatura.getId())).thenReturn(Optional.of(fatura));

        assertThatThrownBy(() -> useCase.executar(fatura.getId(), usuarioId))
                .isInstanceOf(FaturaAindaAbertaException.class);
        verify(accountServiceClient, never()).debitar(any(), any());
    }

    @Test
    void deveriaLancarExcecao_quandoFaturaNaoExiste() {
        UUID id = UUID.randomUUID();
        when(faturaRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, usuarioId))
                .isInstanceOf(FaturaNaoEncontradaException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoFaturaDeOutroUsuario() {
        UUID dono = UUID.randomUUID();
        Cartao cartao = Cartao.criar(dono, "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, contaPagamentoId);
        Fatura fatura = Fatura.criar(cartao.getId(), dono, YearMonth.of(2026, 8), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20));
        when(faturaRepository.buscarPorId(fatura.getId())).thenReturn(Optional.of(fatura));

        assertThatThrownBy(() -> useCase.executar(fatura.getId(), usuarioId))
                .isInstanceOf(FaturaNaoEncontradaException.class);
    }

    @Test
    void naoDeveriaMarcarPaga_quandoDebitoFalhaPorSaldoInsuficiente() {
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, contaPagamentoId);
        Fatura fatura = Fatura.criar(cartao.getId(), usuarioId, YearMonth.of(2026, 8), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20));
        fatura.adicionarParcela(new BigDecimal("150.00"));
        fatura.fechar();
        when(faturaRepository.buscarPorId(fatura.getId())).thenReturn(Optional.of(fatura));
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
        doThrow(new SaldoInsuficienteException(contaPagamentoId)).when(accountServiceClient).debitar(any(), any());

        assertThatThrownBy(() -> useCase.executar(fatura.getId(), usuarioId))
                .isInstanceOf(SaldoInsuficienteException.class);

        assertThat(fatura.isPaga()).isFalse();
        verify(faturaRepository, never()).salvar(any());
    }
}
