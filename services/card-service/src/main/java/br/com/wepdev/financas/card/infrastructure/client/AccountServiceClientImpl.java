package br.com.wepdev.financas.card.infrastructure.client;

import br.com.wepdev.financas.card.domain.AccountServiceClient;
import br.com.wepdev.financas.card.domain.ContaNaoEncontradaException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.UUID;

@ApplicationScoped
public class AccountServiceClientImpl implements AccountServiceClient {

    @RestClient
    AccountServiceUsuarioClient usuarioClient;

    /** Reusa o 404 do account-service (conta inexistente OU de outro usuário) como gate de autorização. */
    @Override
    public void confirmarPosseDaConta(UUID contaId) {
        try {
            usuarioClient.buscarPorId(contaId);
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new ContaNaoEncontradaException(contaId);
            }
            throw e;
        }
    }
}
