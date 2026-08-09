package br.com.wepdev.financas.card.infrastructure.persistence;

import br.com.wepdev.financas.card.domain.Parcela;
import br.com.wepdev.financas.card.domain.ParcelaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ParcelaRepositoryImpl implements ParcelaRepository {

    private final ParcelaPanacheRepository panacheRepository;

    public ParcelaRepositoryImpl(ParcelaPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    /** Parcela é imutável — sempre insere, nunca precisa de upsert (não existe fluxo de editar parcela). */
    @Override
    public void salvar(Parcela parcela) {
        panacheRepository.persist(ParcelaMapper.paraNovaEntidade(parcela));
    }

    @Override
    public List<Parcela> listarPorFatura(UUID faturaId) {
        return panacheRepository.list("faturaId = ?1 order by numeroParcela", faturaId).stream()
                .map(ParcelaMapper::paraDominio)
                .toList();
    }
}
