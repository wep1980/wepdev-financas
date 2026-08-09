package br.com.wepdev.financas.card.infrastructure.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/**
 * Repassa o Authorization do request que chegou (token do usuário final)
 * pro AccountServiceUsuarioClient — a checagem de posse da conta no
 * account-service usa o `sub` desse token, não algo que o card-service
 * decide sozinho.
 */
@ApplicationScoped
public class PropagarAutorizacaoHeadersFactory implements ClientHeadersFactory {

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                   MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> resultado = new MultivaluedHashMap<>();
        String autorizacao = incomingHeaders.getFirst(HttpHeaders.AUTHORIZATION);
        if (autorizacao != null) {
            resultado.add(HttpHeaders.AUTHORIZATION, autorizacao);
        }
        return resultado;
    }
}
