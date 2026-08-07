package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.AccountServiceClient;
import br.com.wepdev.financas.transaction.domain.SaldoInsuficienteException;
import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoEventPublisher;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RegistrarTransacaoUseCaseTest {

    private final TransacaoRepository transacaoRepository = mock(TransacaoRepository.class);
    private final TransacaoEventPublisher eventPublisher = mock(TransacaoEventPublisher.class);
    private final AccountServiceClient accountServiceClient = mock(AccountServiceClient.class);
    private final RegistrarTransacaoUseCase useCase =
            new RegistrarTransacaoUseCase(transacaoRepository, eventPublisher, accountServiceClient);

    private RegistrarTransacaoCommand comandoDespesa;
    private RegistrarTransacaoCommand comandoReceita;

    @BeforeEach
    void setUp() {
        UUID contaId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        comandoDespesa = new RegistrarTransacaoCommand(
                contaId, usuarioId, "Mercado", new BigDecimal("100.00"), TipoTransacao.DESPESA, "Alimentação", null
        );
        comandoReceita = new RegistrarTransacaoCommand(
                contaId, usuarioId, "Salário", new BigDecimal("5000.00"), TipoTransacao.RECEITA, "Salário", null
        );
    }

    @Test
    void deveriaDebitarSalvarEPublicar_quandoDespesa() {
        Transacao transacao = useCase.executar(comandoDespesa);

        verify(accountServiceClient).debitar(comandoDespesa.contaId(), comandoDespesa.valor());
        verify(transacaoRepository).salvar(transacao);
        verify(eventPublisher).publicarTransacaoRegistrada(transacao);
    }

    @Test
    void deveriaCreditarSalvarEPublicar_quandoReceita() {
        Transacao transacao = useCase.executar(comandoReceita);

        verify(accountServiceClient).creditar(comandoReceita.contaId(), comandoReceita.valor());
        verify(transacaoRepository).salvar(transacao);
        verify(eventPublisher).publicarTransacaoRegistrada(transacao);
    }

    @Test
    void naoDeveriaSalvarNemPublicar_quandoAccountServiceFalha() {
        doThrow(new SaldoInsuficienteException(comandoDespesa.contaId()))
                .when(accountServiceClient).debitar(any(), any());

        assertThatThrownBy(() -> useCase.executar(comandoDespesa))
                .isInstanceOf(SaldoInsuficienteException.class);

        verifyNoInteractions(transacaoRepository);
        verifyNoInteractions(eventPublisher);
    }
}
