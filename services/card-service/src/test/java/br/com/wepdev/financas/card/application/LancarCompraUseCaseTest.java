package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Bandeira;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import br.com.wepdev.financas.card.domain.ParcelaRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LancarCompraUseCaseTest {

    private final CartaoRepository cartaoRepository = mock(CartaoRepository.class);
    private final FaturaRepository faturaRepository = mock(FaturaRepository.class);
    private final ParcelaRepository parcelaRepository = mock(ParcelaRepository.class);
    private final LancarCompraUseCase useCase = new LancarCompraUseCase(cartaoRepository, faturaRepository, parcelaRepository);

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID contaPagamentoId = UUID.randomUUID();

    @Test
    void deveriaLancarCompraAVista_naFaturaDoMesCorrente_quandoAntesDoFechamento() {
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, contaPagamentoId);
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
        when(faturaRepository.buscarPorCartaoECompetencia(eq(cartao.getId()), any())).thenReturn(Optional.empty());

        LancarCompraCommand command = new LancarCompraCommand(
                cartao.getId(), usuarioId, "Mercado", new BigDecimal("100.00"), "Alimentação",
                LocalDate.of(2026, 8, 5), 1
        );

        CompraResultado resultado = useCase.executar(command);

        assertThat(resultado.parcelas()).hasSize(1);
        assertThat(resultado.parcelas().get(0).competencia()).isEqualTo(YearMonth.of(2026, 8));
        assertThat(resultado.parcelas().get(0).valor()).isEqualByComparingTo("100.00");
        verify(parcelaRepository).salvar(any());
        verify(faturaRepository, times(2)).salvar(any());
    }

    @Test
    void deveriaLancarCompra_naFaturaDoMesSeguinte_quandoNoOuAposDiaDeFechamento() {
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, contaPagamentoId);
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
        when(faturaRepository.buscarPorCartaoECompetencia(eq(cartao.getId()), any())).thenReturn(Optional.empty());

        LancarCompraCommand command = new LancarCompraCommand(
                cartao.getId(), usuarioId, "Mercado", new BigDecimal("100.00"), "Alimentação",
                LocalDate.of(2026, 8, 10), 1
        );

        CompraResultado resultado = useCase.executar(command);

        assertThat(resultado.parcelas().get(0).competencia()).isEqualTo(YearMonth.of(2026, 9));
    }

    @Test
    void deveriaDistribuirParcelasEmFaturasConsecutivas_comArredondamentoNaUltima() {
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, contaPagamentoId);
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
        when(faturaRepository.buscarPorCartaoECompetencia(eq(cartao.getId()), any())).thenReturn(Optional.empty());

        LancarCompraCommand command = new LancarCompraCommand(
                cartao.getId(), usuarioId, "Notebook", new BigDecimal("100.00"), "Eletrônicos",
                LocalDate.of(2026, 8, 5), 3
        );

        CompraResultado resultado = useCase.executar(command);

        assertThat(resultado.parcelas()).hasSize(3);
        assertThat(resultado.parcelas().get(0).valor()).isEqualByComparingTo("33.33");
        assertThat(resultado.parcelas().get(1).valor()).isEqualByComparingTo("33.33");
        assertThat(resultado.parcelas().get(2).valor()).isEqualByComparingTo("33.34");
        BigDecimal soma = resultado.parcelas().stream().map(ParcelaGerada::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo("100.00");
        assertThat(resultado.parcelas().get(0).competencia()).isEqualTo(YearMonth.of(2026, 8));
        assertThat(resultado.parcelas().get(1).competencia()).isEqualTo(YearMonth.of(2026, 9));
        assertThat(resultado.parcelas().get(2).competencia()).isEqualTo(YearMonth.of(2026, 10));
    }

    @Test
    void deveriaReusarFaturaExistente_quandoJaHaFaturaParaMesmaCompetencia() {
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, contaPagamentoId);
        YearMonth competencia = YearMonth.of(2026, 8);
        Fatura faturaExistente = Fatura.criar(cartao.getId(), usuarioId, competencia, LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 20));
        faturaExistente.adicionarParcela(new BigDecimal("50.00"));
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
        when(faturaRepository.buscarPorCartaoECompetencia(cartao.getId(), competencia)).thenReturn(Optional.of(faturaExistente));

        LancarCompraCommand command = new LancarCompraCommand(
                cartao.getId(), usuarioId, "Mercado", new BigDecimal("30.00"), "Alimentação",
                LocalDate.of(2026, 8, 5), 1
        );

        CompraResultado resultado = useCase.executar(command);

        assertThat(resultado.parcelas().get(0).faturaId()).isEqualTo(faturaExistente.getId());
        assertThat(faturaExistente.getValorTotal()).isEqualByComparingTo("80.00");
    }

    @Test
    void deveriaClampearDiaDeFechamentoNoUltimoDiaDoMes_quandoMesTem28Dias() {
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 31, 10, contaPagamentoId);
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
        when(faturaRepository.buscarPorCartaoECompetencia(eq(cartao.getId()), any())).thenReturn(Optional.empty());

        LancarCompraCommand command = new LancarCompraCommand(
                cartao.getId(), usuarioId, "Mercado", new BigDecimal("10.00"), "Alimentação",
                LocalDate.of(2026, 2, 5), 1
        );

        useCase.executar(command);

        var captor = org.mockito.ArgumentCaptor.forClass(Fatura.class);
        verify(faturaRepository, times(2)).salvar(captor.capture());
        Fatura faturaCriada = captor.getAllValues().get(0);
        assertThat(faturaCriada.getDataFechamento()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void deveriaLancarExcecao_quandoCartaoNaoExiste() {
        UUID cartaoId = UUID.randomUUID();
        when(cartaoRepository.buscarPorId(cartaoId)).thenReturn(Optional.empty());

        LancarCompraCommand command = new LancarCompraCommand(
                cartaoId, usuarioId, "Mercado", new BigDecimal("10.00"), "Alimentação", LocalDate.of(2026, 8, 5), 1
        );

        assertThatThrownBy(() -> useCase.executar(command))
                .isInstanceOf(CartaoNaoEncontradoException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoCartaoDeOutroUsuario() {
        Cartao cartao = Cartao.criar(UUID.randomUUID(), "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, contaPagamentoId);
        when(cartaoRepository.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

        LancarCompraCommand command = new LancarCompraCommand(
                cartao.getId(), usuarioId, "Mercado", new BigDecimal("10.00"), "Alimentação", LocalDate.of(2026, 8, 5), 1
        );

        assertThatThrownBy(() -> useCase.executar(command))
                .isInstanceOf(CartaoNaoEncontradoException.class);
    }
}
