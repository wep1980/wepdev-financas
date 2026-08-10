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
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Alimentação");

        assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.PENDENTE);
        assertThat(lancamento.getDocumentoId()).isEqualTo(documentoId);
        assertThat(lancamento.getId()).isNotNull();
    }

    @Test
    void deveriaLancarExcecao_quandoValorZeroOuNegativo() {
        assertThatThrownBy(() -> LancamentoPendente.extrair(documentoId, "Compra", BigDecimal.ZERO,
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LancamentoPendente.extrair(documentoId, "Compra", new BigDecimal("-10.00"),
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoDescricaoVazia() {
        assertThatThrownBy(() -> LancamentoPendente.extrair(documentoId, "  ", new BigDecimal("10.00"),
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaConfirmar() {
        LancamentoPendente lancamento = LancamentoPendente.extrair(documentoId, "Farmácia", new BigDecimal("30.00"),
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Saúde");

        lancamento.confirmar();

        assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.CONFIRMADO);
    }

    @Test
    void deveriaRejeitar() {
        LancamentoPendente lancamento = LancamentoPendente.extrair(documentoId, "Farmácia", new BigDecimal("30.00"),
                LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Saúde");

        lancamento.rejeitar();

        assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.REJEITADO);
    }

    @Test
    void deveriaReconstituirComOsMesmosValores() {
        UUID id = UUID.randomUUID();
        LancamentoPendente lancamento = LancamentoPendente.reconstituir(id, documentoId, "Farmácia",
                new BigDecimal("30.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Saúde",
                StatusLancamento.CONFIRMADO);

        assertThat(lancamento.getId()).isEqualTo(id);
        assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.CONFIRMADO);
    }
}
