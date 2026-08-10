package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.Orcamento;
import br.com.wepdev.financas.budget.domain.OrcamentoNaoEncontradoException;
import br.com.wepdev.financas.budget.domain.OrcamentoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExcluirOrcamentoUseCaseTest {

    private final OrcamentoRepository orcamentoRepository = mock(OrcamentoRepository.class);
    private final ExcluirOrcamentoUseCase useCase = new ExcluirOrcamentoUseCase(orcamentoRepository);

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void deveriaCancelarOrcamento() {
        Orcamento orcamento = Orcamento.criar(usuarioId, "Mercado", YearMonth.of(2026, 8), new BigDecimal("800.00"));
        when(orcamentoRepository.buscarPorId(orcamento.getId())).thenReturn(Optional.of(orcamento));

        useCase.executar(orcamento.getId(), usuarioId);

        assertThat(orcamento.isAtivo()).isFalse();
        verify(orcamentoRepository).salvar(orcamento);
    }

    @Test
    void deveriaSerIdempotente_quandoOrcamentoJaCancelado() {
        Orcamento orcamento = Orcamento.criar(usuarioId, "Mercado", YearMonth.of(2026, 8), new BigDecimal("800.00"));
        orcamento.cancelar();
        when(orcamentoRepository.buscarPorId(orcamento.getId())).thenReturn(Optional.of(orcamento));

        useCase.executar(orcamento.getId(), usuarioId);

        verify(orcamentoRepository, never()).salvar(orcamento);
    }

    @Test
    void deveriaLancarExcecao_quandoOrcamentoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(orcamentoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, usuarioId))
                .isInstanceOf(OrcamentoNaoEncontradoException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoOrcamentoPertenceAOutroUsuario() {
        Orcamento orcamento = Orcamento.criar(UUID.randomUUID(), "Mercado", YearMonth.of(2026, 8), new BigDecimal("800.00"));
        when(orcamentoRepository.buscarPorId(orcamento.getId())).thenReturn(Optional.of(orcamento));

        assertThatThrownBy(() -> useCase.executar(orcamento.getId(), usuarioId))
                .isInstanceOf(OrcamentoNaoEncontradoException.class);
    }
}
