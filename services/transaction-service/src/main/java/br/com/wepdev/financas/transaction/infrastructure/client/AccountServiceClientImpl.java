package br.com.wepdev.financas.transaction.infrastructure.client;

import br.com.wepdev.financas.transaction.domain.AccountServiceClient;
import br.com.wepdev.financas.transaction.domain.ContaNaoEncontradaException;
import br.com.wepdev.financas.transaction.domain.SaldoInsuficienteException;
import br.com.wepdev.financas.transaction.infrastructure.client.dto.AjusteSaldoRequestDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@ApplicationScoped
public class AccountServiceClientImpl implements AccountServiceClient {

    @RestClient
    AccountServiceUsuarioClient usuarioClient;

    @RestClient
    AccountServiceInternoClient internoClient;

    @Override
    public void debitar(UUID contaId, BigDecimal valor) {
        confirmarPosse(contaId);
        try {
            internoClient.debitar(contaId, new AjusteSaldoRequestDto(valor));
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 422) {
                throw new SaldoInsuficienteException(contaId);
            }
            throw e;
        }
    }

    @Override
    public void creditar(UUID contaId, BigDecimal valor) {
        confirmarPosse(contaId);
        internoClient.creditar(contaId, new AjusteSaldoRequestDto(valor));
    }

    @Override
    public void debitarSemConfirmarPosse(UUID contaId, BigDecimal valor) {
        try {
            internoClient.debitar(contaId, new AjusteSaldoRequestDto(valor));
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 422) {
                throw new SaldoInsuficienteException(contaId);
            }
            throw e;
        }
    }

    @Override
    public void creditarSemConfirmarPosse(UUID contaId, BigDecimal valor) {
        internoClient.creditar(contaId, new AjusteSaldoRequestDto(valor));
    }

    /** Reusa o 404 do account-service (conta inexistente OU de outro usuário) como gate de autorização. */
    private void confirmarPosse(UUID contaId) {
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
