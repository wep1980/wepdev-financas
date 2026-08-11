package br.com.wepdev.financas.ai.infrastructure.client;

import br.com.wepdev.financas.ai.domain.Cartao;
import br.com.wepdev.financas.ai.domain.CardServiceClient;
import br.com.wepdev.financas.ai.domain.CompraResumo;
import br.com.wepdev.financas.ai.domain.Fatura;
import br.com.wepdev.financas.ai.domain.Parcela;
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

    @Override
    public List<Parcela> buscarParcelasDaFatura(UUID faturaId) {
        return restClient.buscarFatura(faturaId).parcelas().stream()
                .map(dto -> new Parcela(dto.compraId(), dto.descricao(), dto.categoria(), dto.numeroParcela(),
                        dto.quantidadeParcelas(), dto.valor()))
                .toList();
    }

    @Override
    public List<CompraResumo> listarCompras(UUID cartaoId) {
        return restClient.listarCompras(cartaoId).stream()
                .map(dto -> new CompraResumo(dto.compraId(), dto.descricao(), dto.categoria(), dto.valorParcela(),
                        dto.quantidadeParcelas(), dto.parcelasRestantes(), dto.valorTotalRestante(), dto.finalizada()))
                .toList();
    }
}
