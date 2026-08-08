package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.FrequenciaRecorrencia;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProximosVencimentosUseCaseTest {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository = mock(TransacaoRecorrenteRepository.class);
    private final ProximosVencimentosUseCase useCase = new ProximosVencimentosUseCase(transacaoRecorrenteRepository);

    @Test
    void deveriaIncluirRegra_quandoVencimentoDentroDaJanela() {
        TransacaoRecorrente regra = TransacaoRecorrente.criar(UUID.randomUUID(), UUID.randomUUID(), "Aluguel",
                new BigDecimal("1500.00"), TipoTransacao.DESPESA, "Moradia", FrequenciaRecorrencia.MENSAL,
                LocalDate.of(2026, 1, 20), null);
        when(transacaoRecorrenteRepository.listarAtivas()).thenReturn(List.of(regra));

        List<ProximoVencimento> resultado = useCase.executar(LocalDate.of(2026, 1, 15), 7);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).dataVencimentoPrevista()).isEqualTo(LocalDate.of(2026, 1, 20));
    }

    @Test
    void naoDeveriaIncluirRegra_quandoVencimentoForaDaJanela() {
        TransacaoRecorrente regra = TransacaoRecorrente.criar(UUID.randomUUID(), UUID.randomUUID(), "Aluguel",
                new BigDecimal("1500.00"), TipoTransacao.DESPESA, "Moradia", FrequenciaRecorrencia.MENSAL,
                LocalDate.of(2026, 2, 20), null);
        when(transacaoRecorrenteRepository.listarAtivas()).thenReturn(List.of(regra));

        List<ProximoVencimento> resultado = useCase.executar(LocalDate.of(2026, 1, 15), 7);

        assertThat(resultado).isEmpty();
    }

    @Test
    void naoDeveriaIncluirRegra_quandoVencimentoJaPassou() {
        TransacaoRecorrente regra = TransacaoRecorrente.criar(UUID.randomUUID(), UUID.randomUUID(), "Aluguel",
                new BigDecimal("1500.00"), TipoTransacao.DESPESA, "Moradia", FrequenciaRecorrencia.MENSAL,
                LocalDate.of(2026, 1, 10), null);
        when(transacaoRecorrenteRepository.listarAtivas()).thenReturn(List.of(regra));

        List<ProximoVencimento> resultado = useCase.executar(LocalDate.of(2026, 1, 15), 7);

        assertThat(resultado).isEmpty();
    }
}
