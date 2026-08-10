package br.com.wepdev.financas.ai.infrastructure.vectorstore;

import br.com.wepdev.financas.ai.infrastructure.vectorstore.dto.QdrantCriarColecaoRequestDto;
import br.com.wepdev.financas.ai.infrastructure.vectorstore.dto.QdrantVetorConfigDto;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Garante que a coleção do Qdrant existe antes do primeiro uso — cria
 * com a dimensão do modelo de embedding configurado
 * (ai-service.llm.ollama.embedding-model) se ainda não existir.
 * Idempotente: se já existe, não faz nada.
 */
@ApplicationScoped
public class QdrantColecaoInicializador {

    private static final Logger LOG = Logger.getLogger(QdrantColecaoInicializador.class);
    private static final String DISTANCIA = "Cosine";

    private final QdrantRestClient restClient;
    private final String colecao;
    private final int dimensaoVetor;

    public QdrantColecaoInicializador(@RestClient QdrantRestClient restClient,
                                       @ConfigProperty(name = "ai-service.qdrant.colecao") String colecao,
                                       @ConfigProperty(name = "ai-service.qdrant.dimensao-vetor") int dimensaoVetor) {
        this.restClient = restClient;
        this.colecao = colecao;
        this.dimensaoVetor = dimensaoVetor;
    }

    void aoIniciar(@Observes StartupEvent evento) {
        try {
            restClient.verificarColecao(colecao);
            LOG.infof("Coleção Qdrant '%s' já existe", colecao);
        } catch (WebApplicationException e) {
            // O REST client do Quarkus lança ClientWebApplicationException (subtipo
            // de WebApplicationException) pra qualquer erro HTTP, não especificamente
            // jakarta.ws.rs.NotFoundException — status precisa ser checado na mão
            // (achado real: catch (NotFoundException) nunca disparava, testado via
            // @QuarkusTest, ver docs/historico.md).
            if (e.getResponse().getStatus() != 404) {
                throw e;
            }
            LOG.infof("Coleção Qdrant '%s' não existe, criando (dimensão=%d, distância=%s)", colecao, dimensaoVetor, DISTANCIA);
            restClient.criarColecao(colecao, new QdrantCriarColecaoRequestDto(new QdrantVetorConfigDto(dimensaoVetor, DISTANCIA)));
        }
    }
}
