package br.com.wepdev.financas.ai.infrastructure.vectorstore;

import br.com.wepdev.financas.ai.domain.RegistroIndexado;
import br.com.wepdev.financas.ai.domain.ResultadoBusca;
import br.com.wepdev.financas.ai.domain.VectorStore;
import br.com.wepdev.financas.ai.infrastructure.vectorstore.dto.QdrantBuscarRequestDto;
import br.com.wepdev.financas.ai.infrastructure.vectorstore.dto.QdrantCondicaoDto;
import br.com.wepdev.financas.ai.infrastructure.vectorstore.dto.QdrantFiltroDto;
import br.com.wepdev.financas.ai.infrastructure.vectorstore.dto.QdrantMatchDto;
import br.com.wepdev.financas.ai.infrastructure.vectorstore.dto.QdrantPontoDto;
import br.com.wepdev.financas.ai.infrastructure.vectorstore.dto.QdrantUpsertPontosRequestDto;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class QdrantVectorStoreImpl implements VectorStore {

    private static final String CAMPO_USUARIO_ID = "usuarioId";
    private static final String CAMPO_TEXTO = "texto";

    private final QdrantRestClient restClient;
    private final String colecao;

    public QdrantVectorStoreImpl(@RestClient QdrantRestClient restClient,
                                  @ConfigProperty(name = "ai-service.qdrant.colecao") String colecao) {
        this.restClient = restClient;
        this.colecao = colecao;
    }

    @Override
    public void indexar(RegistroIndexado registro) {
        Map<String, Object> payload = Map.of(
                CAMPO_USUARIO_ID, registro.usuarioId().toString(),
                CAMPO_TEXTO, registro.texto()
        );
        QdrantPontoDto ponto = new QdrantPontoDto(registro.id().toString(), registro.vetor(), payload);
        restClient.upsertPontos(colecao, true, new QdrantUpsertPontosRequestDto(List.of(ponto)));
    }

    @Override
    public List<ResultadoBusca> buscarSimilares(UUID usuarioId, List<Float> vetorConsulta, int limite) {
        QdrantFiltroDto filtro = new QdrantFiltroDto(
                List.of(new QdrantCondicaoDto(CAMPO_USUARIO_ID, new QdrantMatchDto(usuarioId.toString())))
        );
        var resposta = restClient.buscar(colecao, new QdrantBuscarRequestDto(vetorConsulta, limite, filtro, true));
        return resposta.result().stream()
                .map(resultado -> new ResultadoBusca(
                        UUID.fromString(resultado.id()),
                        String.valueOf(resultado.payload().get(CAMPO_TEXTO)),
                        resultado.score()
                ))
                .toList();
    }
}
