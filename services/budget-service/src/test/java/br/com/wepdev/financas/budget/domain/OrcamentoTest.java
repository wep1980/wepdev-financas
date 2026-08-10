package br.com.wepdev.financas.budget.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrcamentoTest {

    private final UUID usuarioId = UUID.randomUUID();
    private final YearMonth mesReferencia = YearMonth.of(2026, 8);

    @Test
    void deveriaCriarOrcamentoAtivo_quandoDadosValidos() {
        Orcamento orcamento = Orcamento.criar(usuarioId, "Mercado", mesReferencia, new BigDecimal("800.00"));

        assertThat(orcamento.isAtivo()).isTrue();
        assertThat(orcamento.getId()).isNotNull();
        assertThat(orcamento.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(orcamento.getCategoria()).isEqualTo("Mercado");
        assertThat(orcamento.getMesReferencia()).isEqualTo(mesReferencia);
        assertThat(orcamento.getValorLimite()).isEqualByComparingTo("800.00");
        assertThat(orcamento.getCriadoEm()).isNotNull();
    }

    @Test
    void deveriaLancarExcecao_quandoCategoriaVazia() {
        assertThatThrownBy(() -> Orcamento.criar(usuarioId, "  ", mesReferencia, new BigDecimal("100.00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Orcamento.criar(usuarioId, null, mesReferencia, new BigDecimal("100.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoValorLimiteZeroOuNegativo() {
        assertThatThrownBy(() -> Orcamento.criar(usuarioId, "Mercado", mesReferencia, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Orcamento.criar(usuarioId, "Mercado", mesReferencia, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaAtualizarLimite() {
        Orcamento orcamento = Orcamento.criar(usuarioId, "Mercado", mesReferencia, new BigDecimal("800.00"));

        orcamento.atualizarLimite(new BigDecimal("1000.00"));

        assertThat(orcamento.getValorLimite()).isEqualByComparingTo("1000.00");
    }

    @Test
    void deveriaLancarExcecao_quandoAtualizarComLimiteInvalido() {
        Orcamento orcamento = Orcamento.criar(usuarioId, "Mercado", mesReferencia, new BigDecimal("800.00"));

        assertThatThrownBy(() -> orcamento.atualizarLimite(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaCancelarOrcamento_eSerIdempotente() {
        Orcamento orcamento = Orcamento.criar(usuarioId, "Mercado", mesReferencia, new BigDecimal("800.00"));

        orcamento.cancelar();
        assertThat(orcamento.isAtivo()).isFalse();
        assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.CANCELADO);

        orcamento.cancelar();
        assertThat(orcamento.isAtivo()).isFalse();
    }

    @Test
    void deveriaReconstituirOrcamentoExistente() {
        UUID id = UUID.randomUUID();
        Orcamento orcamento = Orcamento.reconstituir(id, usuarioId, "Mercado", mesReferencia,
                new BigDecimal("800.00"), StatusOrcamento.CANCELADO, java.time.Instant.now());

        assertThat(orcamento.getId()).isEqualTo(id);
        assertThat(orcamento.isAtivo()).isFalse();
    }
}
