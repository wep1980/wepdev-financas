package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.Orcamento;
import br.com.wepdev.financas.budget.domain.OrcamentoRepository;
import br.com.wepdev.financas.budget.domain.ResumoCategoria;
import br.com.wepdev.financas.budget.domain.TransactionServiceClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarOrcamentosUseCaseTest {

    private final OrcamentoRepository orcamentoRepository = mock(OrcamentoRepository.class);
    private final TransactionServiceClient transactionServiceClient = mock(TransactionServiceClient.class);
    private final ListarOrcamentosUseCase useCase = new ListarOrcamentosUseCase(orcamentoRepository, transactionServiceClient);

    private final UUID usuarioId = UUID.randomUUID();
    private final YearMonth mesReferencia = YearMonth.of(2026, 8);

    @Test
    void deveriaListarOrcamentos_comValorConsumidoPorCategoria_chamandoResumoUmaSoVez() {
        Orcamento mercado = Orcamento.criar(usuarioId, "Mercado", mesReferencia, new BigDecimal("800.00"));
        Orcamento lazer = Orcamento.criar(usuarioId, "Lazer", mesReferencia, new BigDecimal("300.00"));
        when(orcamentoRepository.listarAtivos(usuarioId, mesReferencia)).thenReturn(List.of(mercado, lazer));
        when(transactionServiceClient.buscarResumoPorCategoria(mesReferencia.atDay(1), mesReferencia.atEndOfMonth()))
                .thenReturn(List.of(new ResumoCategoria("Mercado", new BigDecimal("150.00"))));

        List<OrcamentoDetalhe> resultado = useCase.executar(usuarioId, mesReferencia);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).anySatisfy(detalhe -> {
            assertThat(detalhe.orcamento().getCategoria()).isEqualTo("Mercado");
            assertThat(detalhe.valorConsumido()).isEqualByComparingTo("150.00");
        });
        assertThat(resultado).anySatisfy(detalhe -> {
            assertThat(detalhe.orcamento().getCategoria()).isEqualTo("Lazer");
            assertThat(detalhe.valorConsumido()).isEqualByComparingTo("0");
        });
        verify(transactionServiceClient).buscarResumoPorCategoria(mesReferencia.atDay(1), mesReferencia.atEndOfMonth());
    }

    @Test
    void naoDeveriaChamarTransactionService_quandoNenhumOrcamentoAtivo() {
        when(orcamentoRepository.listarAtivos(usuarioId, mesReferencia)).thenReturn(List.of());

        List<OrcamentoDetalhe> resultado = useCase.executar(usuarioId, mesReferencia);

        assertThat(resultado).isEmpty();
        verify(transactionServiceClient, never()).buscarResumoPorCategoria(any(), any());
    }
}
