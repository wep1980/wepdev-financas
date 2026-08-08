package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.FrequenciaRecorrencia;
import br.com.wepdev.financas.transaction.domain.StatusTransacaoRecorrente;
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

class ListarTransacoesRecorrentesUseCaseTest {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository = mock(TransacaoRecorrenteRepository.class);
    private final ListarTransacoesRecorrentesUseCase useCase = new ListarTransacoesRecorrentesUseCase(transacaoRecorrenteRepository);

    @Test
    void deveriaRetornarRegrasDoUsuario() {
        UUID usuarioId = UUID.randomUUID();
        TransacaoRecorrente regra = TransacaoRecorrente.criar(UUID.randomUUID(), usuarioId, "Salário",
                new BigDecimal("5000.00"), TipoTransacao.RECEITA, "Salário", FrequenciaRecorrencia.MENSAL,
                LocalDate.of(2026, 1, 1), null);
        when(transacaoRecorrenteRepository.listar(usuarioId, null)).thenReturn(List.of(regra));

        List<TransacaoRecorrente> resultado = useCase.executar(usuarioId, null);

        assertThat(resultado).containsExactly(regra);
    }

    @Test
    void deveriaFiltrarPorStatus() {
        UUID usuarioId = UUID.randomUUID();
        when(transacaoRecorrenteRepository.listar(usuarioId, StatusTransacaoRecorrente.ATIVA)).thenReturn(List.of());

        List<TransacaoRecorrente> resultado = useCase.executar(usuarioId, StatusTransacaoRecorrente.ATIVA);

        assertThat(resultado).isEmpty();
    }
}
