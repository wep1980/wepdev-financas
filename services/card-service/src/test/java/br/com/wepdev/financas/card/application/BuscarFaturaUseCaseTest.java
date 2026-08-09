package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaNaoEncontradaException;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import br.com.wepdev.financas.card.domain.Parcela;
import br.com.wepdev.financas.card.domain.ParcelaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuscarFaturaUseCaseTest {

    private final FaturaRepository faturaRepository = mock(FaturaRepository.class);
    private final ParcelaRepository parcelaRepository = mock(ParcelaRepository.class);
    private final BuscarFaturaUseCase useCase = new BuscarFaturaUseCase(faturaRepository, parcelaRepository);

    @Test
    void deveriaRetornarFaturaComParcelas_quandoDonoConfere() {
        UUID usuarioId = UUID.randomUUID();
        Fatura fatura = Fatura.criar(UUID.randomUUID(), usuarioId, YearMonth.of(2026, 8), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20));
        Parcela parcela = Parcela.criar(fatura.getId(), UUID.randomUUID(), "Mercado", new java.math.BigDecimal("10.00"), "Alimentação", 1, 1);
        when(faturaRepository.buscarPorId(fatura.getId())).thenReturn(Optional.of(fatura));
        when(parcelaRepository.listarPorFatura(fatura.getId())).thenReturn(List.of(parcela));

        FaturaDetalhe detalhe = useCase.executar(fatura.getId(), usuarioId);

        assertThat(detalhe.fatura()).isEqualTo(fatura);
        assertThat(detalhe.parcelas()).containsExactly(parcela);
    }

    @Test
    void deveriaLancarExcecao_quandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(faturaRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, UUID.randomUUID()))
                .isInstanceOf(FaturaNaoEncontradaException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoDeOutroUsuario() {
        UUID dono = UUID.randomUUID();
        Fatura fatura = Fatura.criar(UUID.randomUUID(), dono, YearMonth.of(2026, 8), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20));
        when(faturaRepository.buscarPorId(fatura.getId())).thenReturn(Optional.of(fatura));

        assertThatThrownBy(() -> useCase.executar(fatura.getId(), UUID.randomUUID()))
                .isInstanceOf(FaturaNaoEncontradaException.class);
    }
}
