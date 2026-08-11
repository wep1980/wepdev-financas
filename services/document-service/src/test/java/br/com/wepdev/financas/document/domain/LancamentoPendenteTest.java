package br.com.wepdev.financas.document.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LancamentoPendenteTest {

    private final UUID documentoId = UUID.randomUUID();

    @Test
    void deveriaExtrairComoPendente() {
        LancamentoPendente lancamento = LancamentoPendente.extrair(documentoId, "Supermercado", new BigDecimal("150.00"),
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Alimentação", 1, 1);

        assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.PENDENTE);
        assertThat(lancamento.getDocumentoId()).isEqualTo(documentoId);
        assertThat(lancamento.getId()).isNotNull();
        assertThat(lancamento.isParcelado()).isFalse();
    }

    @Test
    void deveriaMarcarComoParcelado_quandoQuantidadeParcelasMaiorQueUm() {
        LancamentoPendente lancamento = LancamentoPendente.extrair(documentoId, "Notebook - Parcela 3/12",
                new BigDecimal("100.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Eletrônicos", 3, 12);

        assertThat(lancamento.isParcelado()).isTrue();
        assertThat(lancamento.getNumeroParcela()).isEqualTo(3);
        assertThat(lancamento.getQuantidadeParcelas()).isEqualTo(12);
    }

    @Test
    void deveriaLancarExcecao_quandoValorZeroOuNegativo() {
        assertThatThrownBy(() -> LancamentoPendente.extrair(documentoId, "Compra", BigDecimal.ZERO,
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LancamentoPendente.extrair(documentoId, "Compra", new BigDecimal("-10.00"),
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoDescricaoVazia() {
        assertThatThrownBy(() -> LancamentoPendente.extrair(documentoId, "  ", new BigDecimal("10.00"),
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoNumeroParcelaForaDoIntervalo() {
        assertThatThrownBy(() -> LancamentoPendente.extrair(documentoId, "Compra", new BigDecimal("10.00"),
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null, 0, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LancamentoPendente.extrair(documentoId, "Compra", new BigDecimal("10.00"),
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null, 4, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoQuantidadeParcelasMenorQueUm() {
        assertThatThrownBy(() -> LancamentoPendente.extrair(documentoId, "Compra", new BigDecimal("10.00"),
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaConfirmar() {
        LancamentoPendente lancamento = LancamentoPendente.extrair(documentoId, "Farmácia", new BigDecimal("30.00"),
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Saúde", 1, 1);

        lancamento.confirmar();

        assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.CONFIRMADO);
    }

    @Test
    void deveriaRejeitar() {
        LancamentoPendente lancamento = LancamentoPendente.extrair(documentoId, "Farmácia", new BigDecimal("30.00"),
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Saúde", 1, 1);

        lancamento.rejeitar();

        assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.REJEITADO);
    }

    @Test
    void deveriaReconstituirComOsMesmosValores() {
        UUID id = UUID.randomUUID();
        LancamentoPendente lancamento = LancamentoPendente.reconstituir(id, documentoId, "Farmácia",
                new BigDecimal("30.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Saúde", 1, 1,
                StatusLancamento.CONFIRMADO);

        assertThat(lancamento.getId()).isEqualTo(id);
        assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.CONFIRMADO);
    }
}
