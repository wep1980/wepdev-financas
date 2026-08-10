package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.Orcamento;
import br.com.wepdev.financas.budget.domain.OrcamentoNaoEncontradoException;
import br.com.wepdev.financas.budget.domain.OrcamentoRepository;
import br.com.wepdev.financas.budget.domain.ResumoCategoria;
import br.com.wepdev.financas.budget.domain.TransactionServiceClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

@ApplicationScoped
public class AtualizarOrcamentoUseCase {

    private final OrcamentoRepository orcamentoRepository;
    private final TransactionServiceClient transactionServiceClient;

    public AtualizarOrcamentoUseCase(OrcamentoRepository orcamentoRepository, TransactionServiceClient transactionServiceClient) {
        this.orcamentoRepository = orcamentoRepository;
        this.transactionServiceClient = transactionServiceClient;
    }

    @Transactional
    public OrcamentoDetalhe executar(AtualizarOrcamentoCommand command) {
        Orcamento orcamento = orcamentoRepository.buscarPorId(command.id())
                .filter(o -> o.getUsuarioId().equals(command.usuarioId()))
                .orElseThrow(() -> new OrcamentoNaoEncontradoException(command.id()));

        orcamento.atualizarLimite(command.novoValorLimite());
        orcamentoRepository.salvar(orcamento);

        BigDecimal valorConsumido = transactionServiceClient
                .buscarResumoPorCategoria(orcamento.getMesReferencia().atDay(1), orcamento.getMesReferencia().atEndOfMonth())
                .stream()
                .filter(resumo -> resumo.categoria().equals(orcamento.getCategoria()))
                .map(ResumoCategoria::totalGasto)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        return new OrcamentoDetalhe(orcamento, valorConsumido);
    }
}
