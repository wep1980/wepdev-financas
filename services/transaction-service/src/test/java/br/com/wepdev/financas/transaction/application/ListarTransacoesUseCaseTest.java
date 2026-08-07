package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoFiltro;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarTransacoesUseCaseTest {

    private final TransacaoRepository transacaoRepository = mock(TransacaoRepository.class);
    private final ListarTransacoesUseCase useCase = new ListarTransacoesUseCase(transacaoRepository);

    @Test
    void deveriaRetornarTransacoesDoFiltro() {
        UUID usuarioId = UUID.randomUUID();
        TransacaoFiltro filtro = new TransacaoFiltro(usuarioId, null, null, null);
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "Mercado", new BigDecimal("50.00"),
                TipoTransacao.DESPESA, "Alimentação", null);
        when(transacaoRepository.listar(filtro)).thenReturn(List.of(transacao));

        List<Transacao> resultado = useCase.executar(filtro);

        assertThat(resultado).containsExactly(transacao);
    }

    @Test
    void deveriaRetornarListaVazia_quandoUsuarioSemTransacoes() {
        TransacaoFiltro filtro = new TransacaoFiltro(UUID.randomUUID(), null, null, null);
        when(transacaoRepository.listar(filtro)).thenReturn(List.of());

        List<Transacao> resultado = useCase.executar(filtro);

        assertThat(resultado).isEmpty();
    }
}
