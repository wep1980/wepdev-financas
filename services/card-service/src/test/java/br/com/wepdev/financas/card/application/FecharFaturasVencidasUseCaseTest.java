package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FecharFaturasVencidasUseCaseTest {

    private final FaturaRepository faturaRepository = mock(FaturaRepository.class);
    private final FecharFaturasVencidasUseCase useCase = new FecharFaturasVencidasUseCase(faturaRepository);

    @Test
    void deveriaFecharFaturasAbertasVencidas() {
        LocalDate hoje = LocalDate.of(2026, 8, 10);
        Fatura fatura = Fatura.criar(UUID.randomUUID(), UUID.randomUUID(), YearMonth.of(2026, 8),
                LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 12));
        fatura.adicionarParcela(new BigDecimal("100.00"));
        when(faturaRepository.listarAbertasVencidas(hoje)).thenReturn(List.of(fatura));

        int fechadas = useCase.executar(hoje);

        assertThat(fechadas).isEqualTo(1);
        assertThat(fatura.isAberta()).isFalse();
        verify(faturaRepository).salvar(fatura);
    }

    @Test
    void naoDeveriaFecharNada_quandoNaoHaFaturaVencida() {
        LocalDate hoje = LocalDate.of(2026, 8, 10);
        when(faturaRepository.listarAbertasVencidas(hoje)).thenReturn(List.of());

        int fechadas = useCase.executar(hoje);

        assertThat(fechadas).isZero();
        verify(faturaRepository, never()).salvar(org.mockito.ArgumentMatchers.any());
    }
}
