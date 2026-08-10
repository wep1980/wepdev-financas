package br.com.wepdev.financas.ai.infrastructure.client;

import br.com.wepdev.financas.ai.domain.AccountServiceClient;
import br.com.wepdev.financas.ai.domain.Conta;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@ApplicationScoped
public class AccountServiceClientImpl implements AccountServiceClient {

    private final AccountServiceRestClient restClient;

    public AccountServiceClientImpl(@RestClient AccountServiceRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<Conta> buscarContasAtivas() {
        return restClient.listarAtivas().stream()
                .map(dto -> new Conta(dto.id(), dto.nome()))
                .toList();
    }
}
