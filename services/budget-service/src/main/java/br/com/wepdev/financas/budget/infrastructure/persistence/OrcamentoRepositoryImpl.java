package br.com.wepdev.financas.budget.infrastructure.persistence;

import br.com.wepdev.financas.budget.domain.Orcamento;
import br.com.wepdev.financas.budget.domain.OrcamentoRepository;
import br.com.wepdev.financas.budget.domain.StatusOrcamento;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrcamentoRepositoryImpl implements OrcamentoRepository {

    private final OrcamentoPanacheRepository panacheRepository;

    public OrcamentoRepositoryImpl(OrcamentoPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public void salvar(Orcamento orcamento) {
        OrcamentoEntity entity = panacheRepository.findById(orcamento.getId());
        if (entity == null) {
            panacheRepository.persist(OrcamentoMapper.paraNovaEntidade(orcamento));
        } else {
            OrcamentoMapper.atualizarEntidade(entity, orcamento);
        }
    }

    @Override
    public Optional<Orcamento> buscarPorId(UUID id) {
        return panacheRepository.findByIdOptional(id).map(OrcamentoMapper::paraDominio);
    }

    @Override
    public List<Orcamento> listarAtivos(UUID usuarioId, YearMonth mesReferencia) {
        return panacheRepository.list("usuarioId = ?1 and mesReferencia = ?2 and status = ?3 order by categoria",
                        usuarioId, mesReferencia.toString(), StatusOrcamento.ATIVO)
                .stream()
                .map(OrcamentoMapper::paraDominio)
                .toList();
    }

    @Override
    public boolean existeAtivo(UUID usuarioId, String categoria, YearMonth mesReferencia) {
        return panacheRepository.count("usuarioId = ?1 and categoria = ?2 and mesReferencia = ?3 and status = ?4",
                usuarioId, categoria, mesReferencia.toString(), StatusOrcamento.ATIVO) > 0;
    }
}
