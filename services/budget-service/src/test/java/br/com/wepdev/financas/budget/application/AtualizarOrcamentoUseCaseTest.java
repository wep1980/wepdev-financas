package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.Orcamento;
import br.com.wepdev.financas.budget.domain.OrcamentoNaoEncontradoException;
import br.com.wepdev.financas.budget.domain.OrcamentoRepository;
import br.com.wepdev.financas.budget.domain.ResumoCategoria;
import br.com.wepdev.financas.budget.domain.TransactionServiceClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtualizarOrcamentoUseCaseTest {

    private final OrcamentoRepository orcamentoRepository = mock(OrcamentoRepository.class);
    private final TransactionServiceClient transactionServiceClient = mock(TransactionServiceClient.class);
    private final AtualizarOrcamentoUseCase useCase = new AtualizarOrcamentoUseCase(orcamentoRepository, transactionServiceClient);

    private final UUID usuarioId = UUID.randomUUID();
    private final YearMonth mesReferencia = YearMonth.of(2026, 8);

    @Test
    void deveriaAtualizarLimite_eRecalcularValorConsumido() {
        Orcamento orcamento = Orcamento.criar(usuarioId, "Mercado", mesReferencia, new BigDecimal("800.00"));
        when(orcamentoRepository.buscarPorId(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(transactionServiceClient.buscarResumoPorCategoria(mesReferencia.atDay(1), mesReferencia.atEndOfMonth()))
                .thenReturn(List.of(new ResumoCategoria("Mercado", new BigDecimal("200.00"))));
        AtualizarOrcamentoCommand command = new AtualizarOrcamentoCommand(orcamento.getId(), usuarioId, new BigDecimal("1000.00"));

        OrcamentoDetalhe detalhe = useCase.executar(command);

        assertThat(detalhe.orcamento().getValorLimite()).isEqualByComparingTo("1000.00");
        assertThat(detalhe.valorConsumido()).isEqualByComparingTo("200.00");
        verify(orcamentoRepository).salvar(orcamento);
    }

    @Test
    void deveriaLancarExcecao_quandoOrcamentoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(orcamentoRepository.buscarPorId(id)).thenReturn(Optional.empty());
        AtualizarOrcamentoCommand command = new AtualizarOrcamentoCommand(id, usuarioId, new BigDecimal("1000.00"));

        assertThatThrownBy(() -> useCase.executar(command))
                .isInstanceOf(OrcamentoNaoEncontradoException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoOrcamentoPertenceAOutroUsuario() {
        Orcamento orcamento = Orcamento.criar(UUID.randomUUID(), "Mercado", mesReferencia, new BigDecimal("800.00"));
        when(orcamentoRepository.buscarPorId(orcamento.getId())).thenReturn(Optional.of(orcamento));
        AtualizarOrcamentoCommand command = new AtualizarOrcamentoCommand(orcamento.getId(), usuarioId, new BigDecimal("1000.00"));

        assertThatThrownBy(() -> useCase.executar(command))
                .isInstanceOf(OrcamentoNaoEncontradoException.class);
    }
}
