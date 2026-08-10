package br.com.wepdev.financas.ai.application;

import br.com.wepdev.financas.ai.domain.EmbeddingResult;
import br.com.wepdev.financas.ai.domain.LlmProvider;
import br.com.wepdev.financas.ai.domain.LlmProviderFactory;
import br.com.wepdev.financas.ai.domain.RegistroIndexado;
import br.com.wepdev.financas.ai.domain.VectorStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexarTransacaoUseCaseTest {

    private final LlmProviderFactory llmProviderFactory = mock(LlmProviderFactory.class);
    private final LlmProvider llmProvider = mock(LlmProvider.class);
    private final VectorStore vectorStore = mock(VectorStore.class);
    private final IndexarTransacaoUseCase useCase = new IndexarTransacaoUseCase(llmProviderFactory, vectorStore);

    @Test
    void deveriaEmbedarADescricaoEIndexarNoVectorStore() {
        UUID transacaoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(llmProviderFactory.criarParaEmbedding()).thenReturn(llmProvider);
        when(llmProvider.embed("Supermercado Pão de Açúcar")).thenReturn(new EmbeddingResult(List.of(0.1f, 0.2f, 0.3f)));

        useCase.executar(new IndexarTransacaoComando(transacaoId, usuarioId, "Supermercado Pão de Açúcar"));

        var captor = forClass(RegistroIndexado.class);
        verify(vectorStore).indexar(captor.capture());
        RegistroIndexado registro = captor.getValue();
        assertThat(registro.id()).isEqualTo(transacaoId);
        assertThat(registro.usuarioId()).isEqualTo(usuarioId);
        assertThat(registro.texto()).isEqualTo("Supermercado Pão de Açúcar");
        assertThat(registro.vetor()).containsExactly(0.1f, 0.2f, 0.3f);
    }
}
