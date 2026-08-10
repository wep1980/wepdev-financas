package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.Orcamento;
import br.com.wepdev.financas.budget.domain.OrcamentoJaExisteException;
import br.com.wepdev.financas.budget.domain.OrcamentoRepository;
import br.com.wepdev.financas.budget.domain.ResumoCategoria;
import br.com.wepdev.financas.budget.domain.TransactionServiceClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

@ApplicationScoped
public class CriarOrcamentoUseCase {

    private final OrcamentoRepository orcamentoRepository;
    private final TransactionServiceClient transactionServiceClient;

    public CriarOrcamentoUseCase(OrcamentoRepository orcamentoRepository, TransactionServiceClient transactionServiceClient) {
        this.orcamentoRepository = orcamentoRepository;
        this.transactionServiceClient = transactionServiceClient;
    }

    /** Rejeita duplicata (mesma categoria+mês ainda ATIVO) ANTES de persistir — ver ADR-0026. */
    @Transactional
    public OrcamentoDetalhe executar(CriarOrcamentoCommand command) {
        if (orcamentoRepository.existeAtivo(command.usuarioId(), command.categoria(), command.mesReferencia())) {
            throw new OrcamentoJaExisteException(command.categoria(), command.mesReferencia());
        }

        Orcamento orcamento = Orcamento.criar(command.usuarioId(), command.categoria(), command.mesReferencia(),
                command.valorLimite());
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
