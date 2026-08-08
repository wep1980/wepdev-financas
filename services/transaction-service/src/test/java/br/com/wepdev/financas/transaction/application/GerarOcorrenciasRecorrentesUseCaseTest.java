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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GerarOcorrenciasRecorrentesUseCaseTest {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository = mock(TransacaoRecorrenteRepository.class);
    private final TransacaoRepository transacaoRepository = mock(TransacaoRepository.class);
    private final TransacaoEventPublisher eventPublisher = mock(TransacaoEventPublisher.class);
    private final AccountServiceClient accountServiceClient = mock(AccountServiceClient.class);
    private final RegistrarTransacaoUseCase registrarTransacaoUseCase =
            new RegistrarTransacaoUseCase(transacaoRepository, eventPublisher, accountServiceClient);
    private final GerarOcorrenciasRecorrentesUseCase useCase =
            new GerarOcorrenciasRecorrentesUseCase(transacaoRecorrenteRepository, registrarTransacaoUseCase);

    private final UUID contaId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void deveriaGerarOcorrencia_quandoRegraVencida() {
        TransacaoRecorrente regra = TransacaoRecorrente.criar(contaId, usuarioId, "Aluguel", new BigDecimal("1500.00"),
                TipoTransacao.DESPESA, "Moradia", FrequenciaRecorrencia.MENSAL, LocalDate.of(2026, 1, 15), null);
        when(transacaoRecorrenteRepository.listarAtivas()).thenReturn(List.of(regra));

        int geradas = useCase.executar(LocalDate.of(2026, 1, 15));

        assertThat(geradas).isEqualTo(1);
        assertThat(regra.getOcorrenciasGeradas()).isEqualTo(1);
        verify(accountServiceClient).debitar(contaId, new BigDecimal("1500.00"));
        verify(transacaoRecorrenteRepository).salvar(regra);
    }

    @Test
    void naoDeveriaGerarOcorrencia_quandoRegraAindaNaoVenceu() {
        TransacaoRecorrente regra = TransacaoRecorrente.criar(contaId, usuarioId, "Aluguel", new BigDecimal("1500.00"),
                TipoTransacao.DESPESA, "Moradia", FrequenciaRecorrencia.MENSAL, LocalDate.of(2026, 3, 1), null);
        when(transacaoRecorrenteRepository.listarAtivas()).thenReturn(List.of(regra));

        int geradas = useCase.executar(LocalDate.of(2026, 1, 15));

        assertThat(geradas).isZero();
        assertThat(regra.getOcorrenciasGeradas()).isZero();
        verify(accountServiceClient, never()).debitar(any(), any());
        verify(transacaoRecorrenteRepository, never()).salvar(any());
    }

    @Test
    void deveriaConcluirRegra_quandoAtingeQuantidadeOcorrenciasNestaExecucao() {
        TransacaoRecorrente regra = TransacaoRecorrente.reconstituir(UUID.randomUUID(), contaId, usuarioId,
                "Financiamento", new BigDecimal("500.00"), TipoTransacao.DESPESA, "Financiamento",
                FrequenciaRecorrencia.MENSAL, LocalDate.of(2026, 1, 1), 3, 2,
                StatusTransacaoRecorrente.ATIVA, Instant.now());
        when(transacaoRecorrenteRepository.listarAtivas()).thenReturn(List.of(regra));

        useCase.executar(LocalDate.of(2026, 3, 1));

        assertThat(regra.getOcorrenciasGeradas()).isEqualTo(3);
        assertThat(regra.getStatus()).isEqualTo(StatusTransacaoRecorrente.CONCLUIDA);
    }

    @Test
    void naoDeveriaConcluirSozinha_quandoRegraIndefinida_apósVariasExecucoes() {
        TransacaoRecorrente regra = TransacaoRecorrente.criar(contaId, usuarioId, "Salário", new BigDecimal("5000.00"),
                TipoTransacao.RECEITA, "Salário", FrequenciaRecorrencia.MENSAL, LocalDate.of(2026, 1, 1), null);
        when(transacaoRecorrenteRepository.listarAtivas()).thenReturn(List.of(regra));

        LocalDate hoje = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < 24; i++) {
            useCase.executar(hoje);
            hoje = hoje.plusMonths(1);
        }

        assertThat(regra.getOcorrenciasGeradas()).isEqualTo(24);
        assertThat(regra.isAtiva()).isTrue();
    }

    @Test
    void naoDeveriaGerarNada_quandoNaoHaRegrasAtivas() {
        when(transacaoRecorrenteRepository.listarAtivas()).thenReturn(List.of());

        int geradas = useCase.executar(LocalDate.now());

        assertThat(geradas).isZero();
        verify(accountServiceClient, never()).debitar(any(), any());
        verify(accountServiceClient, never()).creditar(any(), any());
    }
}
