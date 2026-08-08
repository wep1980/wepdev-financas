package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.AccountServiceClient;
import br.com.wepdev.financas.transaction.domain.SaldoInsuficienteException;
import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoCanceladaException;
import br.com.wepdev.financas.transaction.domain.TransacaoNaoEncontradaException;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AtualizarTransacaoUseCaseTest {

    private final TransacaoRepository transacaoRepository = mock(TransacaoRepository.class);
    private final AccountServiceClient accountServiceClient = mock(AccountServiceClient.class);
    private final AtualizarTransacaoUseCase useCase = new AtualizarTransacaoUseCase(transacaoRepository, accountServiceClient);

    @Test
    void deveriaDebitarDelta_quandoDespesaAumentaDeValor() {
        UUID usuarioId = UUID.randomUUID();
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", null);
        when(transacaoRepository.buscarPorId(transacao.getId())).thenReturn(Optional.of(transacao));

        useCase.executar(new AtualizarTransacaoCommand(transacao.getId(), usuarioId, "Mercado maior",
                new BigDecimal("150.00"), "Alimentação", null));

        verify(accountServiceClient).debitar(transacao.getContaId(), new BigDecimal("50.00"));
        verify(accountServiceClient, never()).creditar(any(), any());
        assertThat(transacao.getValor()).isEqualByComparingTo("150.00");
        assertThat(transacao.getDescricao()).isEqualTo("Mercado maior");
        verify(transacaoRepository).salvar(transacao);
    }

    @Test
    void deveriaCreditarDelta_quandoDespesaDiminuiDeValor() {
        UUID usuarioId = UUID.randomUUID();
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", null);
        when(transacaoRepository.buscarPorId(transacao.getId())).thenReturn(Optional.of(transacao));

        useCase.executar(new AtualizarTransacaoCommand(transacao.getId(), usuarioId, "Mercado menor",
                new BigDecimal("60.00"), "Alimentação", null));

        verify(accountServiceClient).creditar(transacao.getContaId(), new BigDecimal("40.00"));
        verify(accountServiceClient, never()).debitar(any(), any());
        assertThat(transacao.getValor()).isEqualByComparingTo("60.00");
    }

    @Test
    void deveriaCreditarDelta_quandoReceitaAumentaDeValor() {
        UUID usuarioId = UUID.randomUUID();
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "Salário", new BigDecimal("5000.00"),
                TipoTransacao.RECEITA, "Salário", null);
        when(transacaoRepository.buscarPorId(transacao.getId())).thenReturn(Optional.of(transacao));

        useCase.executar(new AtualizarTransacaoCommand(transacao.getId(), usuarioId, "Salário",
                new BigDecimal("5500.00"), "Salário", null));

        verify(accountServiceClient).creditar(transacao.getContaId(), new BigDecimal("500.00"));
        verify(accountServiceClient, never()).debitar(any(), any());
    }

    @Test
    void deveriaDebitarDelta_quandoReceitaDiminuiDeValor() {
        UUID usuarioId = UUID.randomUUID();
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "Salário", new BigDecimal("5000.00"),
                TipoTransacao.RECEITA, "Salário", null);
        when(transacaoRepository.buscarPorId(transacao.getId())).thenReturn(Optional.of(transacao));

        useCase.executar(new AtualizarTransacaoCommand(transacao.getId(), usuarioId, "Salário",
                new BigDecimal("4500.00"), "Salário", null));

        verify(accountServiceClient).debitar(transacao.getContaId(), new BigDecimal("500.00"));
        verify(accountServiceClient, never()).creditar(any(), any());
    }

    @Test
    void naoDeveriaChamarAccountService_quandoValorNaoMuda() {
        UUID usuarioId = UUID.randomUUID();
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", null);
        when(transacaoRepository.buscarPorId(transacao.getId())).thenReturn(Optional.of(transacao));

        useCase.executar(new AtualizarTransacaoCommand(transacao.getId(), usuarioId, "Mercado renomeado",
                new BigDecimal("100.00"), "Alimentação", null));

        verifyNoInteractions(accountServiceClient);
        assertThat(transacao.getDescricao()).isEqualTo("Mercado renomeado");
        verify(transacaoRepository).salvar(transacao);
    }

    @Test
    void deveriaLancarExcecao_quandoTransacaoJaCancelada() {
        UUID usuarioId = UUID.randomUUID();
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", null);
        transacao.cancelar();
        when(transacaoRepository.buscarPorId(transacao.getId())).thenReturn(Optional.of(transacao));

        assertThatThrownBy(() -> useCase.executar(new AtualizarTransacaoCommand(transacao.getId(), usuarioId,
                "Mercado", new BigDecimal("150.00"), "Alimentação", null)))
                .isInstanceOf(TransacaoCanceladaException.class);

        verifyNoInteractions(accountServiceClient);
        verify(transacaoRepository, never()).salvar(any());
    }

    @Test
    void deveriaLancarExcecao_quandoTransacaoNaoExiste() {
        UUID id = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(transacaoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(new AtualizarTransacaoCommand(id, usuarioId,
                "Mercado", new BigDecimal("150.00"), "Alimentação", null)))
                .isInstanceOf(TransacaoNaoEncontradaException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoTransacaoEhDeOutroUsuario() {
        UUID dono = UUID.randomUUID();
        UUID outroUsuario = UUID.randomUUID();
        Transacao transacao = Transacao.criar(UUID.randomUUID(), dono, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", null);
        when(transacaoRepository.buscarPorId(transacao.getId())).thenReturn(Optional.of(transacao));

        assertThatThrownBy(() -> useCase.executar(new AtualizarTransacaoCommand(transacao.getId(), outroUsuario,
                "Mercado", new BigDecimal("150.00"), "Alimentação", null)))
                .isInstanceOf(TransacaoNaoEncontradaException.class);
    }

    @Test
    void naoDeveriaAtualizar_quandoAjusteDeSaldoFalhaPorSaldoInsuficiente() {
        UUID usuarioId = UUID.randomUUID();
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", null);
        when(transacaoRepository.buscarPorId(transacao.getId())).thenReturn(Optional.of(transacao));
        doThrow(new SaldoInsuficienteException(transacao.getContaId()))
                .when(accountServiceClient).debitar(any(), any());

        assertThatThrownBy(() -> useCase.executar(new AtualizarTransacaoCommand(transacao.getId(), usuarioId,
                "Mercado maior", new BigDecimal("150.00"), "Alimentação", null)))
                .isInstanceOf(SaldoInsuficienteException.class);

        assertThat(transacao.getValor()).isEqualByComparingTo("100.00");
        verify(transacaoRepository, never()).salvar(any());
    }
}
