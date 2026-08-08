package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.IntervaloInvalidoException;
import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoFiltro;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResumoPorCategoriaUseCaseTest {

    private final TransacaoRepository transacaoRepository = mock(TransacaoRepository.class);
    private final ResumoPorCategoriaUseCase useCase = new ResumoPorCategoriaUseCase(transacaoRepository);

    private final UUID usuarioId = UUID.randomUUID();
    private final LocalDate inicio = LocalDate.of(2026, 8, 1);
    private final LocalDate fim = LocalDate.of(2026, 8, 31);

    @Test
    void deveriaAgruparPorCategoriaECalcularPercentual() {
        List<Transacao> transacoes = List.of(
                transacao("Alimentação", "80.00", TipoTransacao.DESPESA, false),
                transacao("Alimentação", "20.00", TipoTransacao.DESPESA, false),
                transacao("Transporte", "100.00", TipoTransacao.DESPESA, false)
        );
        when(transacaoRepository.listar(argThat(inicio, fim))).thenReturn(transacoes);
        when(transacaoRepository.listar(argThatPeriodoAnterior())).thenReturn(List.of());

        List<ResumoCategoria> resultado = useCase.executar(usuarioId, inicio, fim);

        assertThat(resultado).hasSize(2);
        ResumoCategoria alimentacao = resultado.stream().filter(r -> r.categoria().equals("Alimentação")).findFirst().orElseThrow();
        assertThat(alimentacao.totalGasto()).isEqualByComparingTo("100.00");
        assertThat(alimentacao.percentualDoTotal()).isEqualByComparingTo("50.00");
        assertThat(alimentacao.totalGastoPeriodoAnterior()).isNull();

        ResumoCategoria transporte = resultado.stream().filter(r -> r.categoria().equals("Transporte")).findFirst().orElseThrow();
        assertThat(transporte.totalGasto()).isEqualByComparingTo("100.00");
        assertThat(transporte.percentualDoTotal()).isEqualByComparingTo("50.00");
    }

    @Test
    void deveriaOrdenarPorTotalGastoDecrescente() {
        List<Transacao> transacoes = List.of(
                transacao("Lazer", "10.00", TipoTransacao.DESPESA, false),
                transacao("Alimentação", "200.00", TipoTransacao.DESPESA, false),
                transacao("Transporte", "50.00", TipoTransacao.DESPESA, false)
        );
        when(transacaoRepository.listar(argThat(inicio, fim))).thenReturn(transacoes);
        when(transacaoRepository.listar(argThatPeriodoAnterior())).thenReturn(List.of());

        List<ResumoCategoria> resultado = useCase.executar(usuarioId, inicio, fim);

        assertThat(resultado).extracting(ResumoCategoria::categoria)
                .containsExactly("Alimentação", "Transporte", "Lazer");
    }

    @Test
    void deveriaIgnorarReceitasETransacoesCanceladas() {
        List<Transacao> transacoes = List.of(
                transacao("Salário", "5000.00", TipoTransacao.RECEITA, false),
                transacao("Alimentação", "50.00", TipoTransacao.DESPESA, true)
        );
        when(transacaoRepository.listar(argThat(inicio, fim))).thenReturn(transacoes);
        when(transacaoRepository.listar(argThatPeriodoAnterior())).thenReturn(List.of());

        List<ResumoCategoria> resultado = useCase.executar(usuarioId, inicio, fim);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveriaUsarSemCategoria_quandoCategoriaNula() {
        List<Transacao> transacoes = List.of(transacao(null, "30.00", TipoTransacao.DESPESA, false));
        when(transacaoRepository.listar(argThat(inicio, fim))).thenReturn(transacoes);
        when(transacaoRepository.listar(argThatPeriodoAnterior())).thenReturn(List.of());

        List<ResumoCategoria> resultado = useCase.executar(usuarioId, inicio, fim);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).categoria()).isEqualTo("Sem categoria");
    }

    @Test
    void devePreencherTotalGastoPeriodoAnterior_quandoCategoriaExistiaAntes() {
        when(transacaoRepository.listar(argThat(inicio, fim)))
                .thenReturn(List.of(transacao("Alimentação", "100.00", TipoTransacao.DESPESA, false)));
        when(transacaoRepository.listar(argThatPeriodoAnterior()))
                .thenReturn(List.of(transacao("Alimentação", "80.00", TipoTransacao.DESPESA, false)));

        List<ResumoCategoria> resultado = useCase.executar(usuarioId, inicio, fim);

        assertThat(resultado.get(0).totalGastoPeriodoAnterior()).isEqualByComparingTo("80.00");
    }

    @Test
    void deveriaLancarExcecao_quandoInicioDepoisDoFim() {
        assertThatThrownBy(() -> useCase.executar(usuarioId, fim, inicio))
                .isInstanceOf(IntervaloInvalidoException.class);
    }

    @Test
    void deveriaRetornarListaVazia_quandoNaoHaDespesasNoPeriodo() {
        when(transacaoRepository.listar(any())).thenReturn(List.of());

        List<ResumoCategoria> resultado = useCase.executar(usuarioId, inicio, fim);

        assertThat(resultado).isEmpty();
    }

    private Transacao transacao(String categoria, String valor, TipoTransacao tipo, boolean cancelada) {
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "Descrição", new BigDecimal(valor), tipo, categoria, inicio);
        if (cancelada) {
            transacao.cancelar();
        }
        return transacao;
    }

    private TransacaoFiltro argThat(LocalDate inicioEsperado, LocalDate fimEsperado) {
        return org.mockito.ArgumentMatchers.argThat(f -> f != null && inicioEsperado.equals(f.inicio()) && fimEsperado.equals(f.fim()));
    }

    private TransacaoFiltro argThatPeriodoAnterior() {
        return org.mockito.ArgumentMatchers.argThat(f -> f != null && f.fim().isBefore(inicio));
    }
}
