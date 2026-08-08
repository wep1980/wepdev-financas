package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.AccountServiceClient;
import br.com.wepdev.financas.transaction.domain.FrequenciaRecorrencia;
import br.com.wepdev.financas.transaction.domain.StatusTransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import br.com.wepdev.financas.transaction.domain.TransacaoEventPublisher;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteRepository;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CriarTransacaoRecorrenteUseCaseTest {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository = mock(TransacaoRecorrenteRepository.class);
    private final TransacaoRepository transacaoRepository = mock(TransacaoRepository.class);
    private final TransacaoEventPublisher eventPublisher = mock(TransacaoEventPublisher.class);
    private final AccountServiceClient accountServiceClient = mock(AccountServiceClient.class);
    private final RegistrarTransacaoUseCase registrarTransacaoUseCase =
            new RegistrarTransacaoUseCase(transacaoRepository, eventPublisher, accountServiceClient);
    private final CriarTransacaoRecorrenteUseCase useCase =
            new CriarTransacaoRecorrenteUseCase(transacaoRecorrenteRepository, registrarTransacaoUseCase);

    private final UUID contaId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final LocalDate dataInicio = LocalDate.of(2026, 1, 15);

    @Test
    void deveriaCriarRegraEGerarPrimeiraOcorrencia() {
        CriarTransacaoRecorrenteCommand command = new CriarTransacaoRecorrenteCommand(
                contaId, usuarioId, "Salário", new BigDecimal("5000.00"), TipoTransacao.RECEITA, "Salário",
                FrequenciaRecorrencia.MENSAL, dataInicio, null
        );

        TransacaoRecorrente regra = useCase.executar(command);

        assertThat(regra.getOcorrenciasGeradas()).isEqualTo(1);
        assertThat(regra.isAtiva()).isTrue();
        verify(accountServiceClient).creditar(contaId, new BigDecimal("5000.00"));
        verify(transacaoRepository).salvar(any());
        verify(transacaoRecorrenteRepository).salvar(regra);
    }

    @Test
    void deveriaNascerConcluida_quandoQuantidadeOcorrenciasEhUm() {
        CriarTransacaoRecorrenteCommand command = new CriarTransacaoRecorrenteCommand(
                contaId, usuarioId, "Pagamento único parcelado em regra", new BigDecimal("100.00"),
                TipoTransacao.DESPESA, "Outros", FrequenciaRecorrencia.MENSAL, dataInicio, 1
        );

        TransacaoRecorrente regra = useCase.executar(command);

        assertThat(regra.getStatus()).isEqualTo(StatusTransacaoRecorrente.CONCLUIDA);
        assertThat(regra.getOcorrenciasGeradas()).isEqualTo(1);
    }
}
