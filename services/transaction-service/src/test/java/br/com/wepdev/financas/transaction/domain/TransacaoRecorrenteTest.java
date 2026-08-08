package br.com.wepdev.financas.transaction.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransacaoRecorrenteTest {

    private final UUID contaId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final LocalDate dataInicio = LocalDate.of(2026, 1, 15);

    @Test
    void deveriaCriarRegraAtiva_quandoDadosValidos() {
        TransacaoRecorrente regra = TransacaoRecorrente.criar(contaId, usuarioId, "Salário", new BigDecimal("5000.00"),
                TipoTransacao.RECEITA, "Salário", FrequenciaRecorrencia.MENSAL, dataInicio, null);

        assertThat(regra.isAtiva()).isTrue();
        assertThat(regra.getOcorrenciasGeradas()).isZero();
        assertThat(regra.getQuantidadeOcorrencias()).isNull();
        assertThat(regra.proximaDataVencimento()).isEqualTo(dataInicio);
    }

    @Test
    void deveriaLancarExcecao_quandoValorZeroOuNegativo() {
        assertThatThrownBy(() -> TransacaoRecorrente.criar(contaId, usuarioId, "Aluguel", BigDecimal.ZERO,
                TipoTransacao.DESPESA, "Moradia", FrequenciaRecorrencia.MENSAL, dataInicio, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoQuantidadeOcorrenciasZeroOuNegativa() {
        assertThatThrownBy(() -> TransacaoRecorrente.criar(contaId, usuarioId, "Assinatura", new BigDecimal("30.00"),
                TipoTransacao.DESPESA, "Lazer", FrequenciaRecorrencia.MENSAL, dataInicio, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaAvancarProximaDataVencimento_conformeOcorrenciasGeradas() {
        TransacaoRecorrente regra = TransacaoRecorrente.criar(contaId, usuarioId, "Aluguel", new BigDecimal("1500.00"),
                TipoTransacao.DESPESA, "Moradia", FrequenciaRecorrencia.MENSAL, dataInicio, null);

        assertThat(regra.proximaDataVencimento()).isEqualTo(LocalDate.of(2026, 1, 15));
        regra.registrarOcorrenciaGerada();
        assertThat(regra.proximaDataVencimento()).isEqualTo(LocalDate.of(2026, 2, 15));
        regra.registrarOcorrenciaGerada();
        assertThat(regra.proximaDataVencimento()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void deveriaConcluirAutomaticamente_quandoAtingeQuantidadeOcorrencias() {
        TransacaoRecorrente regra = TransacaoRecorrente.criar(contaId, usuarioId, "Financiamento", new BigDecimal("500.00"),
                TipoTransacao.DESPESA, "Financiamento", FrequenciaRecorrencia.MENSAL, dataInicio, 2);

        regra.registrarOcorrenciaGerada();
        assertThat(regra.isAtiva()).isTrue();
        regra.registrarOcorrenciaGerada();
        assertThat(regra.isAtiva()).isFalse();
        assertThat(regra.getStatus()).isEqualTo(StatusTransacaoRecorrente.CONCLUIDA);
    }

    @Test
    void naoDeveriaConcluirSozinha_quandoQuantidadeOcorrenciasIndefinida() {
        TransacaoRecorrente regra = TransacaoRecorrente.criar(contaId, usuarioId, "Salário", new BigDecimal("5000.00"),
                TipoTransacao.RECEITA, "Salário", FrequenciaRecorrencia.MENSAL, dataInicio, null);

        for (int i = 0; i < 50; i++) {
            regra.registrarOcorrenciaGerada();
        }

        assertThat(regra.isAtiva()).isTrue();
    }

    @Test
    void deveriaCancelar_eSerIdempotente() {
        TransacaoRecorrente regra = TransacaoRecorrente.criar(contaId, usuarioId, "Assinatura", new BigDecimal("30.00"),
                TipoTransacao.DESPESA, "Lazer", FrequenciaRecorrencia.MENSAL, dataInicio, null);

        regra.cancelar();
        assertThat(regra.isCancelada()).isTrue();

        regra.cancelar();
        assertThat(regra.isCancelada()).isTrue();
    }
}
