package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Bandeira;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProximosVencimentosUseCaseTest {

    private final FaturaRepository faturaRepository = mock(FaturaRepository.class);
    private final CartaoRepository cartaoRepository = mock(CartaoRepository.class);
    private final ProximosVencimentosUseCase useCase = new ProximosVencimentosUseCase(faturaRepository, cartaoRepository);

    @Test
    void deveriaIncluirFatura_quandoVencimentoDentroDaJanela() {
        Cartao cartao = Cartao.criar(UUID.randomUUID(), "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 5, 12, UUID.randomUUID());
        Fatura fatura = Fatura.criar(cartao.getId(), cartao.getUsuarioId(), YearMonth.of(2026, 8),
                LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 20));
        when(faturaRepository.listarFechadas()).thenReturn(List.of(fatura));
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

        List<ProximoVencimentoFatura> resultado = useCase.executar(LocalDate.of(2026, 8, 15), 7);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).apelidoCartao()).isEqualTo("Nubank");
        assertThat(resultado.get(0).dataVencimento()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void naoDeveriaIncluirFatura_quandoVencimentoForaDaJanela() {
        Cartao cartao = Cartao.criar(UUID.randomUUID(), "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 5, 12, UUID.randomUUID());
        Fatura fatura = Fatura.criar(cartao.getId(), cartao.getUsuarioId(), YearMonth.of(2026, 9),
                LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 20));
        when(faturaRepository.listarFechadas()).thenReturn(List.of(fatura));

        List<ProximoVencimentoFatura> resultado = useCase.executar(LocalDate.of(2026, 8, 15), 7);

        assertThat(resultado).isEmpty();
    }

    @Test
    void naoDeveriaIncluirFatura_quandoVencimentoJaPassou() {
        Cartao cartao = Cartao.criar(UUID.randomUUID(), "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 5, 12, UUID.randomUUID());
        Fatura fatura = Fatura.criar(cartao.getId(), cartao.getUsuarioId(), YearMonth.of(2026, 7),
                LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 20));
        when(faturaRepository.listarFechadas()).thenReturn(List.of(fatura));

        List<ProximoVencimentoFatura> resultado = useCase.executar(LocalDate.of(2026, 8, 15), 7);

        assertThat(resultado).isEmpty();
    }
}
