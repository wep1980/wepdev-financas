package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.OrcamentoJaExisteException;
import br.com.wepdev.financas.budget.domain.OrcamentoRepository;
import br.com.wepdev.financas.budget.domain.ResumoCategoria;
import br.com.wepdev.financas.budget.domain.TransactionServiceClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CriarOrcamentoUseCaseTest {

    private final OrcamentoRepository orcamentoRepository = mock(OrcamentoRepository.class);
    private final TransactionServiceClient transactionServiceClient = mock(TransactionServiceClient.class);
    private final CriarOrcamentoUseCase useCase = new CriarOrcamentoUseCase(orcamentoRepository, transactionServiceClient);

    private final UUID usuarioId = UUID.randomUUID();
    private final YearMonth mesReferencia = YearMonth.of(2026, 8);

    @Test
    void deveriaCriarESalvarOrcamento_comValorConsumidoDoTransactionService() {
        when(orcamentoRepository.existeAtivo(usuarioId, "Mercado", mesReferencia)).thenReturn(false);
        when(transactionServiceClient.buscarResumoPorCategoria(mesReferencia.atDay(1), mesReferencia.atEndOfMonth()))
                .thenReturn(List.of(new ResumoCategoria("Mercado", new BigDecimal("150.00")),
                        new ResumoCategoria("Transporte", new BigDecimal("80.00"))));
        CriarOrcamentoCommand command = new CriarOrcamentoCommand(usuarioId, "Mercado", mesReferencia, new BigDecimal("800.00"));

        OrcamentoDetalhe detalhe = useCase.executar(command);

        assertThat(detalhe.orcamento().getCategoria()).isEqualTo("Mercado");
        assertThat(detalhe.orcamento().getValorLimite()).isEqualByComparingTo("800.00");
        assertThat(detalhe.valorConsumido()).isEqualByComparingTo("150.00");
        verify(orcamentoRepository).salvar(detalhe.orcamento());
    }

    @Test
    void deveriaUsarValorConsumidoZero_quandoCategoriaSemGastoNoMes() {
        when(orcamentoRepository.existeAtivo(any(), any(), any())).thenReturn(false);
        when(transactionServiceClient.buscarResumoPorCategoria(any(), any())).thenReturn(List.of());
        CriarOrcamentoCommand command = new CriarOrcamentoCommand(usuarioId, "Lazer", mesReferencia, new BigDecimal("300.00"));

        OrcamentoDetalhe detalhe = useCase.executar(command);

        assertThat(detalhe.valorConsumido()).isEqualByComparingTo("0");
    }

    @Test
    void naoDeveriaSalvar_quandoJaExisteOrcamentoAtivoParaCategoriaEMes() {
        when(orcamentoRepository.existeAtivo(usuarioId, "Mercado", mesReferencia)).thenReturn(true);
        CriarOrcamentoCommand command = new CriarOrcamentoCommand(usuarioId, "Mercado", mesReferencia, new BigDecimal("800.00"));

        assertThatThrownBy(() -> useCase.executar(command))
                .isInstanceOf(OrcamentoJaExisteException.class);
        verify(orcamentoRepository, never()).salvar(any());
    }
}
