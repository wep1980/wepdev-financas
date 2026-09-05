package br.com.wepdev.financas.ai.infrastructure.vectorstore;

import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QdrantHeadersFactoryTest {

    @Test
    void deveEnviarApiKeyConfigurada() {
        var headers = new MultivaluedHashMap<String, String>();

        new QdrantHeadersFactory("chave-producao")
                .update(new MultivaluedHashMap<>(), headers);

        assertThat(headers.getFirst(QdrantHeadersFactory.API_KEY_HEADER))
                .isEqualTo("chave-producao");
    }

    @Test
    void naoDeveEnviarHeaderQuandoApiKeyEstiverVazia() {
        var headers = new MultivaluedHashMap<String, String>();

        new QdrantHeadersFactory("")
                .update(new MultivaluedHashMap<>(), headers);

        assertThat(headers).doesNotContainKey(QdrantHeadersFactory.API_KEY_HEADER);
    }
}
