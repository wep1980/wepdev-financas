package br.com.wepdev.financas.ai.infrastructure.client;

import br.com.wepdev.financas.ai.domain.CriarTransacaoComando;
import br.com.wepdev.financas.ai.domain.CriarTransacaoRecorrenteComando;
import br.com.wepdev.financas.ai.domain.ResumoCategoria;
import br.com.wepdev.financas.ai.domain.Transacao;
import br.com.wepdev.financas.ai.domain.TransactionServiceClient;
import br.com.wepdev.financas.ai.domain.TipoTransacao;
import br.com.wepdev.financas.ai.infrastructure.client.dto.CriarTransacaoRecorrenteRequestDto;
import br.com.wepdev.financas.ai.infrastructure.client.dto.CriarTransacaoRequestDto;
import br.com.wepdev.financas.ai.infrastructure.client.dto.TransacaoDto;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class TransactionServiceClientImpl implements TransactionServiceClient {

    private final TransactionServiceRestClient restClient;

    public TransactionServiceClientImpl(@RestClient TransactionServiceRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<Transacao> buscarTransacoes(LocalDate inicio, LocalDate fim) {
        return restClient.listar(inicio, fim).stream().map(this::paraDominio).toList();
    }

    @Override
    public List<ResumoCategoria> buscarResumoPorCategoria(LocalDate inicio, LocalDate fim) {
        return restClient.resumoPorCategoria(inicio, fim).stream()
                .map(dto -> new ResumoCategoria(dto.categoria(), dto.totalGasto(), dto.percentualDoTotal(),
                        dto.totalGastoPeriodoAnterior()))
                .toList();
    }

    @Override
    public Transacao criarTransacao(CriarTransacaoComando comando) {
        TransacaoDto dto = restClient.criar(new CriarTransacaoRequestDto(comando.contaId(), comando.descricao(),
                comando.valor(), comando.tipo().name(), comando.categoria(), comando.dataTransacao()));
        return paraDominio(dto);
    }

    @Override
    public void criarTransacaoRecorrente(CriarTransacaoRecorrenteComando comando) {
        restClient.criarRecorrente(new CriarTransacaoRecorrenteRequestDto(comando.contaId(), comando.descricao(),
                comando.valor(), comando.tipo().name(), comando.categoria(), comando.frequencia().name(),
                comando.dataInicio(), comando.quantidadeOcorrencias()));
    }

    private Transacao paraDominio(TransacaoDto dto) {
        return new Transacao(dto.id(), dto.contaId(), dto.descricao(), dto.valor(),
                TipoTransacao.valueOf(dto.tipo()), dto.categoria(), dto.dataTransacao());
    }
}
