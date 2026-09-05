package br.com.wepdev.financas.ai.infrastructure.vectorstore;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.Optional;

@ApplicationScoped
public class QdrantHeadersFactory implements ClientHeadersFactory {

    static final String API_KEY_HEADER = "api-key";

    private final Optional<String> apiKey;

    public QdrantHeadersFactory(
            @ConfigProperty(name = "ai-service.qdrant.api-key") Optional<String> apiKey) {
        this.apiKey = apiKey.filter(valor -> !valor.isBlank());
    }

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> outgoingHeaders) {
        apiKey.ifPresent(valor -> outgoingHeaders.putSingle(API_KEY_HEADER, valor));
        return outgoingHeaders;
    }
}
