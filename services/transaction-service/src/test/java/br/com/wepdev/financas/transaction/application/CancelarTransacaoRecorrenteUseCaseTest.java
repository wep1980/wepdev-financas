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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelarTransacaoRecorrenteUseCaseTest {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository = mock(TransacaoRecorrenteRepository.class);
    private final CancelarTransacaoRecorrenteUseCase useCase = new CancelarTransacaoRecorrenteUseCase(transacaoRecorrenteRepository);

    @Test
    void deveriaCancelarRegra() {
        UUID usuarioId = UUID.randomUUID();
        TransacaoRecorrente regra = TransacaoRecorrente.criar(UUID.randomUUID(), usuarioId, "Assinatura",
                new BigDecimal("30.00"), TipoTransacao.DESPESA, "Lazer", FrequenciaRecorrencia.MENSAL,
                LocalDate.of(2026, 1, 1), null);
        when(transacaoRecorrenteRepository.buscarPorId(regra.getId())).thenReturn(Optional.of(regra));

        useCase.executar(regra.getId(), usuarioId);

        assertThat(regra.isCancelada()).isTrue();
        verify(transacaoRecorrenteRepository).salvar(regra);
    }

    @Test
    void naoDeveriaFazerNada_quandoJaCancelada() {
        UUID usuarioId = UUID.randomUUID();
        TransacaoRecorrente regra = TransacaoRecorrente.criar(UUID.randomUUID(), usuarioId, "Assinatura",
                new BigDecimal("30.00"), TipoTransacao.DESPESA, "Lazer", FrequenciaRecorrencia.MENSAL,
                LocalDate.of(2026, 1, 1), null);
        regra.cancelar();
        when(transacaoRecorrenteRepository.buscarPorId(regra.getId())).thenReturn(Optional.of(regra));

        useCase.executar(regra.getId(), usuarioId);

        verify(transacaoRecorrenteRepository, never()).salvar(regra);
    }

    @Test
    void deveriaLancarExcecao_quandoRegraNaoExiste() {
        UUID id = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(transacaoRecorrenteRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, usuarioId))
                .isInstanceOf(TransacaoRecorrenteNaoEncontradaException.class);
    }

    @Test
    void deveriaLancarExcecao_quandoRegraEhDeOutroUsuario() {
        UUID dono = UUID.randomUUID();
        UUID outroUsuario = UUID.randomUUID();
        TransacaoRecorrente regra = TransacaoRecorrente.criar(UUID.randomUUID(), dono, "Assinatura",
                new BigDecimal("30.00"), TipoTransacao.DESPESA, "Lazer", FrequenciaRecorrencia.MENSAL,
                LocalDate.of(2026, 1, 1), null);
        when(transacaoRecorrenteRepository.buscarPorId(regra.getId())).thenReturn(Optional.of(regra));

        assertThatThrownBy(() -> useCase.executar(regra.getId(), outroUsuario))
                .isInstanceOf(TransacaoRecorrenteNaoEncontradaException.class);
    }
}
