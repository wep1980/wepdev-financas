package br.com.wepdev.financas.ai.infrastructure.client;

import br.com.wepdev.financas.ai.domain.Cartao;
import br.com.wepdev.financas.ai.domain.CardServiceClient;
import br.com.wepdev.financas.ai.domain.Fatura;
import br.com.wepdev.financas.ai.domain.StatusFatura;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CardServiceClientImpl implements CardServiceClient {

    private final CardServiceRestClient restClient;

    public CardServiceClientImpl(@RestClient CardServiceRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<Cartao> buscarCartoesAtivos() {
        return restClient.listarCartoesAtivos().stream()
                .map(dto -> new Cartao(dto.id(), dto.apelido()))
                .toList();
    }

    @Override
    public List<Fatura> buscarFaturas(UUID cartaoId, StatusFatura statusFiltro) {
        String status = statusFiltro == null ? null : statusFiltro.name();
        return restClient.listarFaturas(cartaoId, status).stream()
                .map(dto -> new Fatura(dto.id(), dto.competencia(), dto.dataVencimento(), dto.valorTotal(),
                        StatusFatura.valueOf(dto.status())))
                .toList();
    }
}
