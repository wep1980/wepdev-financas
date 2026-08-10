package br.com.wepdev.financas.ai.application;

import br.com.wepdev.financas.ai.domain.EmbeddingResult;
import br.com.wepdev.financas.ai.domain.LlmProvider;
import br.com.wepdev.financas.ai.domain.LlmProviderFactory;
import br.com.wepdev.financas.ai.domain.ResultadoBusca;
import br.com.wepdev.financas.ai.domain.VectorStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuscarTransacoesSimilaresUseCaseTest {

    private final LlmProviderFactory llmProviderFactory = mock(LlmProviderFactory.class);
    private final LlmProvider llmProvider = mock(LlmProvider.class);
    private final VectorStore vectorStore = mock(VectorStore.class);
    private final BuscarTransacoesSimilaresUseCase useCase = new BuscarTransacoesSimilaresUseCase(llmProviderFactory, vectorStore);

    @Test
    void deveriaEmbedarAConsultaEBuscarNoVectorStore() {
        UUID usuarioId = UUID.randomUUID();
        UUID transacaoId = UUID.randomUUID();
        when(llmProviderFactory.criarParaEmbedding()).thenReturn(llmProvider);
        when(llmProvider.embed("gastos com mercado")).thenReturn(new EmbeddingResult(List.of(0.5f, 0.6f)));
        when(vectorStore.buscarSimilares(usuarioId, List.of(0.5f, 0.6f), 5))
                .thenReturn(List.of(new ResultadoBusca(transacaoId, "Supermercado Pão de Açúcar", 0.92f)));

        List<ResultadoBusca> resultado = useCase.executar(usuarioId, "gastos com mercado", 5);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(transacaoId);
        assertThat(resultado.get(0).score()).isEqualTo(0.92f);
    }
}
