package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.AccountServiceClient;
import br.com.wepdev.financas.transaction.domain.SaldoInsuficienteException;
import br.com.wepdev.financas.transaction.domain.Transacao;
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
import static org.mockito.Mockito.when;

class CancelarTransacaoUseCaseTest {

    private final TransacaoRepository transacaoRepository = mock(TransacaoRepository.class);
    private final AccountServiceClient accountServiceClient = mock(AccountServiceClient.class);
    private final CancelarTransacaoUseCase useCase = new CancelarTransacaoUseCase(transacaoRepository, accountServiceClient);

    @Test
    void deveriaCreditarDeVoltaECancelar_quandoTransacaoEraDespesa() {
        UUID usuarioId = UUID.randomUUID();
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", null);
        when(transacaoRepository.buscarPorId(transacao.getId())).thenReturn(Optional.of(transacao));

        useCase.executar(transacao.getId(), usuarioId);

        verify(accountServiceClient).creditar(transacao.getContaId(), new BigDecimal("100.00"));
        assertThat(transacao.isCancelada()).isTrue();
        verify(transacaoRepository).salvar(transacao);
    }

    @Test
    void deveriaDebitarDeVoltaECancelar_quandoTransacaoEraReceita() {
        UUID usuarioId = UUID.randomUUID();
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "Salário", new BigDecimal("5000.00"),
                TipoTransacao.RECEITA, "Salário", null);
        when(transacaoRepository.buscarPorId(transacao.getId())).thenReturn(Optional.of(transacao));

        useCase.executar(transacao.getId(), usuarioId);

        verify(accountServiceClient).debitar(transacao.getContaId(), new BigDecimal("5000.00"));
        assertThat(transacao.isCancelada()).isTrue();
    }

    @Test
    void naoDeveriaFazerNada_quandoTransacaoJaCancelada() {
        UUID usuarioId = UUID.randomUUID();
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", null);
        transacao.cancelar();
        when(transacaoRepository.buscarPorId(transacao.getId())).thenReturn(Optional.of(transacao));

        useCase.executar(transacao.getId(), usuarioId);

        verify(accountServiceClient, never()).creditar(any(), any());
        verify(transacaoRepository, never()).salvar(any());
    }

    @Test
    void deveriaLancarExcecao_quandoTransacaoNaoExiste() {
        UUID id = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(transacaoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, usuarioId))
                .isInstanceOf(TransacaoNaoEncontradaException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoTransacaoEhDeOutroUsuario() {
        UUID dono = UUID.randomUUID();
        UUID outroUsuario = UUID.randomUUID();
        Transacao transacao = Transacao.criar(UUID.randomUUID(), dono, "Mercado", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Alimentação", null);
        when(transacaoRepository.buscarPorId(transacao.getId())).thenReturn(Optional.of(transacao));

        assertThatThrownBy(() -> useCase.executar(transacao.getId(), outroUsuario))
                .isInstanceOf(TransacaoNaoEncontradaException.class);
    }

    @Test
    void naoDeveriaCancelar_quandoReverterReceitaFalhaPorSaldoInsuficiente() {
        UUID usuarioId = UUID.randomUUID();
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "Salário", new BigDecimal("5000.00"),
                TipoTransacao.RECEITA, "Salário", null);
        when(transacaoRepository.buscarPorId(transacao.getId())).thenReturn(Optional.of(transacao));
        doThrow(new SaldoInsuficienteException(transacao.getContaId()))
                .when(accountServiceClient).debitar(any(), any());

        assertThatThrownBy(() -> useCase.executar(transacao.getId(), usuarioId))
                .isInstanceOf(SaldoInsuficienteException.class);

        assertThat(transacao.isCancelada()).isFalse();
        verify(transacaoRepository, never()).salvar(any());
    }
}
