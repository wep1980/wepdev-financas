package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.Orcamento;
import br.com.wepdev.financas.budget.domain.OrcamentoRepository;
import br.com.wepdev.financas.budget.domain.ResumoCategoria;
import br.com.wepdev.financas.budget.domain.TransactionServiceClient;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ListarOrcamentosUseCase {

    private final OrcamentoRepository orcamentoRepository;
    private final TransactionServiceClient transactionServiceClient;

    public ListarOrcamentosUseCase(OrcamentoRepository orcamentoRepository, TransactionServiceClient transactionServiceClient) {
        this.orcamentoRepository = orcamentoRepository;
        this.transactionServiceClient = transactionServiceClient;
    }

    public List<OrcamentoDetalhe> executar(UUID usuarioId, YearMonth mesReferencia) {
        List<Orcamento> orcamentos = orcamentoRepository.listarAtivos(usuarioId, mesReferencia);
        if (orcamentos.isEmpty()) {
            return List.of();
        }

        // Um resumo só pro mês inteiro, reaproveitado por categoria — evita
        // uma chamada ao transaction-service por orçamento na lista.
        Map<String, BigDecimal> gastoPorCategoria = transactionServiceClient
                .buscarResumoPorCategoria(mesReferencia.atDay(1), mesReferencia.atEndOfMonth())
                .stream()
                .collect(Collectors.toMap(ResumoCategoria::categoria, ResumoCategoria::totalGasto));

        return orcamentos.stream()
                .map(orcamento -> new OrcamentoDetalhe(orcamento,
                        gastoPorCategoria.getOrDefault(orcamento.getCategoria(), BigDecimal.ZERO)))
                .toList();
    }
}
