package br.com.wepdev.financas.account.application;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.ContaEventPublisher;
import br.com.wepdev.financas.account.domain.ContaRepository;
import br.com.wepdev.financas.account.domain.TipoConta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CriarContaUseCaseTest {

    private final ContaRepository contaRepository = mock(ContaRepository.class);
    private final ContaEventPublisher eventPublisher = mock(ContaEventPublisher.class);
    private final CriarContaUseCase useCase = new CriarContaUseCase(contaRepository, eventPublisher);

    private CriarContaCommand comandoValido;

    @BeforeEach
    void setUp() {
        comandoValido = new CriarContaCommand(
                UUID.randomUUID(), "Conta corrente", TipoConta.CORRENTE, new BigDecimal("100.00"), "Banco X"
        );
    }

    @Test
    void deveriaSalvarContaEPublicarEvento_quandoComandoValido() {
        Conta contaCriada = useCase.executar(comandoValido);

        assertThat(contaCriada.getUsuarioId()).isEqualTo(comandoValido.usuarioId());
        assertThat(contaCriada.getSaldo()).isEqualByComparingTo(comandoValido.saldoInicial());
        verify(contaRepository).salvar(contaCriada);
        verify(eventPublisher).publicarContaCriada(contaCriada);
    }

    @Test
    void naoDeveriaSalvarNemPublicar_quandoSaldoInicialNegativo() {
        CriarContaCommand comandoInvalido = new CriarContaCommand(
                UUID.randomUUID(), "Conta corrente", TipoConta.CORRENTE, new BigDecimal("-1.00"), "Banco X"
        );

        assertThatThrownBy(() -> useCase.executar(comandoInvalido))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(contaRepository);
        verifyNoInteractions(eventPublisher);
    }
}
