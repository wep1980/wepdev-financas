package br.com.wepdev.financas.budget.infrastructure.client;

import br.com.wepdev.financas.budget.domain.DespesaRecorrente;
import br.com.wepdev.financas.budget.domain.ResumoCategoria;
import br.com.wepdev.financas.budget.domain.TransactionServiceClient;
import br.com.wepdev.financas.budget.infrastructure.client.dto.TransacaoRecorrenteDto;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class TransactionServiceClientImpl implements TransactionServiceClient {

    private static final String STATUS_ATIVA = "ATIVA";
    private static final String TIPO_DESPESA = "DESPESA";

    private final TransactionServiceRestClient restClient;

    public TransactionServiceClientImpl(@RestClient TransactionServiceRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<DespesaRecorrente> buscarDespesasRecorrentesAtivas() {
        return restClient.listarRecorrentes(STATUS_ATIVA).stream()
                .filter(dto -> TIPO_DESPESA.equals(dto.tipo()))
                .map(dto -> new DespesaRecorrente(dto.id(), dto.descricao(), dto.valor(), dto.dataInicio()))
                .toList();
    }

    @Override
    public List<ResumoCategoria> buscarResumoPorCategoria(LocalDate inicio, LocalDate fim) {
        return restClient.resumoPorCategoria(inicio, fim).stream()
                .map(dto -> new ResumoCategoria(dto.categoria(), dto.totalGasto()))
                .toList();
    }
}
