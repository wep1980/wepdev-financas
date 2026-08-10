package br.com.wepdev.financas.budget.infrastructure.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/**
 * Repassa o Authorization do request que chegou (token do usuário final)
 * pros três clientes de saída (account-service/card-service/
 * transaction-service, ADR-0026) — todos os endpoints chamados já
 * filtram pelo `sub` do token, sem precisar de confirmação de posse de
 * um id específico (diferente do padrão em card-service/document-service).
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
