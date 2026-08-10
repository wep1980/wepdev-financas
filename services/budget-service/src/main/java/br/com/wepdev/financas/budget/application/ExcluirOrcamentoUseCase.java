package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.Orcamento;
import br.com.wepdev.financas.budget.domain.OrcamentoNaoEncontradoException;
import br.com.wepdev.financas.budget.domain.OrcamentoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class ExcluirOrcamentoUseCase {

    private final OrcamentoRepository orcamentoRepository;

    public ExcluirOrcamentoUseCase(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    /** Idempotente: excluir de novo um orçamento já cancelado continua sem erro. */
    @Transactional
    public void executar(UUID id, UUID usuarioId) {
        Orcamento orcamento = orcamentoRepository.buscarPorId(id)
                .filter(o -> o.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new OrcamentoNaoEncontradoException(id));

        if (!orcamento.isAtivo()) {
            return;
        }

        orcamento.cancelar();
        orcamentoRepository.salvar(orcamento);
    }
}
