package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Bandeira;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import br.com.wepdev.financas.card.domain.Parcela;
import br.com.wepdev.financas.card.domain.ParcelaRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarComprasUseCaseTest {

    private final CartaoRepository cartaoRepository = mock(CartaoRepository.class);
    private final FaturaRepository faturaRepository = mock(FaturaRepository.class);
    private final ParcelaRepository parcelaRepository = mock(ParcelaRepository.class);
    private final ListarComprasUseCase useCase =
            new ListarComprasUseCase(cartaoRepository, faturaRepository, parcelaRepository);

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID contaPagamentoId = UUID.randomUUID();
    private final Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20,
            contaPagamentoId);

    @Test
    void deveriaAgruparParcelasPorCompra_eContarSoAsRestantesEmFaturaAberta() {
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

        Fatura faturaFechada = Fatura.criar(cartao.getId(), usuarioId, YearMonth.of(2026, 7),
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 20));
        faturaFechada.fechar();
        Fatura faturaAberta = Fatura.criar(cartao.getId(), usuarioId, YearMonth.of(2026, 8),
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20));
        when(faturaRepository.listarPorCartao(cartao.getId(), null)).thenReturn(List.of(faturaFechada, faturaAberta));

        UUID compraId = UUID.randomUUID();
        Parcela parcela1 = Parcela.criar(faturaFechada.getId(), compraId, "Notebook", new BigDecimal("100.00"),
                "Eletrônicos", 1, 3);
        Parcela parcela2 = Parcela.criar(faturaAberta.getId(), compraId, "Notebook", new BigDecimal("100.00"),
                "Eletrônicos", 2, 3);
        when(parcelaRepository.listarPorFatura(faturaFechada.getId())).thenReturn(List.of(parcela1));
        when(parcelaRepository.listarPorFatura(faturaAberta.getId())).thenReturn(List.of(parcela2));

        List<CompraResumo> resumos = useCase.executar(cartao.getId(), usuarioId);

        assertThat(resumos).hasSize(1);
        CompraResumo resumo = resumos.get(0);
        assertThat(resumo.compraId()).isEqualTo(compraId);
        assertThat(resumo.descricao()).isEqualTo("Notebook");
        assertThat(resumo.categoria()).isEqualTo("Eletrônicos");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("100.00");
        assertThat(resumo.quantidadeParcelas()).isEqualTo(3);
        assertThat(resumo.parcelasRestantes()).isEqualTo(1);
        assertThat(resumo.valorTotalRestante()).isEqualByComparingTo("100.00");
        assertThat(resumo.finalizada()).isFalse();
    }

    @Test
    void deveriaMarcarFinalizada_quandoTodasAsParcelasJaEstaoEmFaturaFechadaOuPaga() {
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

        Fatura faturaPaga = Fatura.criar(cartao.getId(), usuarioId, YearMonth.of(2026, 6),
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 20));
        faturaPaga.fechar();
        faturaPaga.pagar();
        when(faturaRepository.listarPorCartao(cartao.getId(), null)).thenReturn(List.of(faturaPaga));

        UUID compraId = UUID.randomUUID();
        Parcela unica = Parcela.criar(faturaPaga.getId(), compraId, "Mercado", new BigDecimal("50.00"),
                "Alimentação", 1, 1);
        when(parcelaRepository.listarPorFatura(faturaPaga.getId())).thenReturn(List.of(unica));

        List<CompraResumo> resumos = useCase.executar(cartao.getId(), usuarioId);

        assertThat(resumos.get(0).finalizada()).isTrue();
        assertThat(resumos.get(0).parcelasRestantes()).isZero();
        assertThat(resumos.get(0).valorTotalRestante()).isEqualByComparingTo("0.00");
    }

    @Test
    void deveriaOrdenarPorDescricao() {
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
        Fatura fatura = Fatura.criar(cartao.getId(), usuarioId, YearMonth.of(2026, 8), LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 20));
        when(faturaRepository.listarPorCartao(cartao.getId(), null)).thenReturn(List.of(fatura));

        Parcela zebra = Parcela.criar(fatura.getId(), UUID.randomUUID(), "Zebra", new BigDecimal("10.00"), null, 1, 1);
        Parcela alfa = Parcela.criar(fatura.getId(), UUID.randomUUID(), "Alfa", new BigDecimal("10.00"), null, 1, 1);
        when(parcelaRepository.listarPorFatura(fatura.getId())).thenReturn(List.of(zebra, alfa));

        List<CompraResumo> resumos = useCase.executar(cartao.getId(), usuarioId);

        assertThat(resumos).extracting(CompraResumo::descricao).containsExactly("Alfa", "Zebra");
    }

    @Test
    void deveriaLancarExcecao_quandoCartaoNaoEncontrado() {
        UUID cartaoId = UUID.randomUUID();
        when(cartaoRepository.buscarPorId(cartaoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(cartaoId, usuarioId))
                .isInstanceOf(CartaoNaoEncontradoException.class);
    }
}
