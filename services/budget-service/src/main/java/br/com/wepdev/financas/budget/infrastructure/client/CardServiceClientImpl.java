package br.com.wepdev.financas.budget.infrastructure.client;

import br.com.wepdev.financas.budget.domain.CardServiceClient;
import br.com.wepdev.financas.budget.domain.FaturaFechada;
import br.com.wepdev.financas.budget.infrastructure.client.dto.CartaoDto;
import br.com.wepdev.financas.budget.infrastructure.client.dto.FaturaDto;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

/**
 * Orquestra duas chamadas (ver ADR-0026): lista os cartões ativos do
 * usuário, depois busca as faturas FECHADA de cada um. Card-service não
 * tem um endpoint "todas as faturas em aberto do usuário" de uma vez só.
 */
@ApplicationScoped
public class CardServiceClientImpl implements CardServiceClient {

    private static final String STATUS_FECHADA = "FECHADA";

    private final CardServiceRestClient restClient;

    public CardServiceClientImpl(@RestClient CardServiceRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<FaturaFechada> buscarFaturasFechadas() {
        List<CartaoDto> cartoes = restClient.listarCartoesAtivos();
        return cartoes.stream()
                .flatMap(cartao -> restClient.listarFaturas(cartao.id(), STATUS_FECHADA).stream()
                        .map(fatura -> new FaturaFechada(fatura.id(), cartao.apelido(), fatura.valorTotal(),
                                fatura.dataVencimento())))
                .toList();
    }
}
