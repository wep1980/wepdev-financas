package br.com.wepdev.financas.ai.infrastructure.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/**
 * Repassa o Authorization do request que chegou (token do usuário final)
 * pros três clientes de saída (budget-service/card-service/
 * transaction-service) — todos os endpoints chamados já filtram pelo
 * `sub` do token, sem precisar de confirmação de posse de um id
 * específico (mesmo padrão do budget-service, ADR-0026).
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
