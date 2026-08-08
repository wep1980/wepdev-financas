package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.FrequenciaRecorrencia;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteNaoEncontradaException;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuscarTransacaoRecorrenteUseCaseTest {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository = mock(TransacaoRecorrenteRepository.class);
    private final BuscarTransacaoRecorrenteUseCase useCase = new BuscarTransacaoRecorrenteUseCase(transacaoRecorrenteRepository);

    @Test
    void deveriaRetornarRegra_quandoDonoConfere() {
        UUID usuarioId = UUID.randomUUID();
        TransacaoRecorrente regra = TransacaoRecorrente.criar(UUID.randomUUID(), usuarioId, "Salário",
                new BigDecimal("5000.00"), TipoTransacao.RECEITA, "Salário", FrequenciaRecorrencia.MENSAL,
                LocalDate.of(2026, 1, 1), null);
        when(transacaoRecorrenteRepository.buscarPorId(regra.getId())).thenReturn(Optional.of(regra));

        assertThat(useCase.executar(regra.getId(), usuarioId)).isEqualTo(regra);
    }

    @Test
    void deveriaLancarExcecao_quandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(transacaoRecorrenteRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, UUID.randomUUID()))
                .isInstanceOf(TransacaoRecorrenteNaoEncontradaException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoDeOutroUsuario() {
        UUID dono = UUID.randomUUID();
        TransacaoRecorrente regra = TransacaoRecorrente.criar(UUID.randomUUID(), dono, "Salário",
                new BigDecimal("5000.00"), TipoTransacao.RECEITA, "Salário", FrequenciaRecorrencia.MENSAL,
                LocalDate.of(2026, 1, 1), null);
        when(transacaoRecorrenteRepository.buscarPorId(regra.getId())).thenReturn(Optional.of(regra));

        assertThatThrownBy(() -> useCase.executar(regra.getId(), UUID.randomUUID()))
                .isInstanceOf(TransacaoRecorrenteNaoEncontradaException.class);
    }
}
