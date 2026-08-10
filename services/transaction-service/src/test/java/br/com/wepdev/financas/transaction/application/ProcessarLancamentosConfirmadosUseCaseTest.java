package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.AccountServiceClient;
import br.com.wepdev.financas.transaction.domain.SaldoInsuficienteException;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import br.com.wepdev.financas.transaction.domain.TransacaoEventPublisher;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ProcessarLancamentosConfirmadosUseCaseTest {

    private final TransacaoRepository transacaoRepository = mock(TransacaoRepository.class);
    private final TransacaoEventPublisher eventPublisher = mock(TransacaoEventPublisher.class);
    private final AccountServiceClient accountServiceClient = mock(AccountServiceClient.class);
    private final ProcessarLancamentosConfirmadosUseCase useCase =
            new ProcessarLancamentosConfirmadosUseCase(transacaoRepository, eventPublisher, accountServiceClient);

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID contaId = UUID.randomUUID();

    @Test
    void deveriaDebitarSemConfirmarPosse_paraCadaDespesa_eSalvarEPublicar() {
        var lancamento = new LancamentoConfirmadoCommand("Mercado", new BigDecimal("50.00"),
                TipoTransacao.DESPESA, "Alimentação", LocalDate.of(2026, 8, 5));

        useCase.executar(new ProcessarLancamentosConfirmadosCommand(usuarioId, contaId, List.of(lancamento)));

        verify(accountServiceClient).debitarSemConfirmarPosse(contaId, new BigDecimal("50.00"));
        verify(accountServiceClient, never()).debitar(any(), any());
        verify(transacaoRepository, times(1)).salvar(any());
        verify(eventPublisher, times(1)).publicarTransacaoRegistrada(any());
    }

    @Test
    void deveriaCreditarSemConfirmarPosse_paraCadaReceita() {
        var lancamento = new LancamentoConfirmadoCommand("Estorno", new BigDecimal("30.00"),
                TipoTransacao.RECEITA, null, LocalDate.of(2026, 8, 6));

        useCase.executar(new ProcessarLancamentosConfirmadosCommand(usuarioId, contaId, List.of(lancamento)));

        verify(accountServiceClient).creditarSemConfirmarPosse(contaId, new BigDecimal("30.00"));
        verify(accountServiceClient, never()).creditar(any(), any());
    }

    @Test
    void deveriaProcessarVariosLancamentos_naMesmaChamada() {
        var despesa = new LancamentoConfirmadoCommand("Mercado", new BigDecimal("50.00"),
                TipoTransacao.DESPESA, "Alimentação", LocalDate.of(2026, 8, 5));
        var receita = new LancamentoConfirmadoCommand("Estorno", new BigDecimal("30.00"),
                TipoTransacao.RECEITA, null, LocalDate.of(2026, 8, 6));

        useCase.executar(new ProcessarLancamentosConfirmadosCommand(usuarioId, contaId, List.of(despesa, receita)));

        verify(transacaoRepository, times(2)).salvar(any());
        verify(eventPublisher, times(2)).publicarTransacaoRegistrada(any());
    }

    @Test
    void naoDeveriaSalvarNemPublicarEsseLancamento_quandoAccountServiceFalha() {
        var lancamento = new LancamentoConfirmadoCommand("Mercado", new BigDecimal("50.00"),
                TipoTransacao.DESPESA, "Alimentação", LocalDate.of(2026, 8, 5));
        doThrow(new SaldoInsuficienteException(contaId))
                .when(accountServiceClient).debitarSemConfirmarPosse(eq(contaId), any());

        assertThatThrownBy(() -> useCase.executar(
                new ProcessarLancamentosConfirmadosCommand(usuarioId, contaId, List.of(lancamento))))
                .isInstanceOf(SaldoInsuficienteException.class);

        verify(transacaoRepository, never()).salvar(any());
        verify(eventPublisher, never()).publicarTransacaoRegistrada(any());
    }
}
