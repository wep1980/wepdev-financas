package br.com.wepdev.financas.ai.infrastructure.vectorstore;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class QdrantHeadersFactory implements ClientHeadersFactory {

    static final String API_KEY_HEADER = "api-key";

    private final String apiKey;

    public QdrantHeadersFactory(
            @ConfigProperty(name = "ai-service.qdrant.api-key") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> outgoingHeaders) {
        if (!apiKey.isBlank()) {
            outgoingHeaders.putSingle(API_KEY_HEADER, apiKey);
        }
        return outgoingHeaders;
    }
}
