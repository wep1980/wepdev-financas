package br.com.wepdev.financas.ai.infrastructure.client;

import br.com.wepdev.financas.ai.domain.BudgetServiceClient;
import br.com.wepdev.financas.ai.domain.DisponivelParaGastar;
import br.com.wepdev.financas.ai.infrastructure.client.dto.DisponivelParaGastarDto;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.YearMonth;

@ApplicationScoped
public class BudgetServiceClientImpl implements BudgetServiceClient {

    private final BudgetServiceRestClient restClient;

    public BudgetServiceClientImpl(@RestClient BudgetServiceRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public DisponivelParaGastar buscarDisponivelParaGastar(YearMonth mes) {
        DisponivelParaGastarDto dto = restClient.disponivelParaGastar(mes.toString());
        return new DisponivelParaGastar(dto.valorDisponivel(), dto.saldoContas(), dto.faturasEmAberto(),
                dto.despesasRecorrentes(), dto.reserva());
    }
}
