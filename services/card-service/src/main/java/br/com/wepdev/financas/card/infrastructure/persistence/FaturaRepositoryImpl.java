package br.com.wepdev.financas.card.infrastructure.persistence;

import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import br.com.wepdev.financas.card.domain.StatusFatura;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FaturaRepositoryImpl implements FaturaRepository {

    private final FaturaPanacheRepository panacheRepository;

    public FaturaRepositoryImpl(FaturaPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public void salvar(Fatura fatura) {
        FaturaEntity entity = panacheRepository.findById(fatura.getId());
        if (entity == null) {
            panacheRepository.persist(FaturaMapper.paraNovaEntidade(fatura));
        } else {
            FaturaMapper.atualizarEntidade(entity, fatura);
        }
    }

    @Override
    public Optional<Fatura> buscarPorId(UUID id) {
        return panacheRepository.findByIdOptional(id).map(FaturaMapper::paraDominio);
    }

    @Override
    public Optional<Fatura> buscarPorCartaoECompetencia(UUID cartaoId, YearMonth competencia) {
        return panacheRepository.find("cartaoId = ?1 and competencia = ?2", cartaoId, competencia.toString())
                .firstResultOptional()
                .map(FaturaMapper::paraDominio);
    }

    @Override
    public List<Fatura> listarPorCartao(UUID cartaoId, StatusFatura status) {
        if (status != null) {
            return panacheRepository.list("cartaoId = ?1 and status = ?2 order by competencia desc", cartaoId, status)
                    .stream().map(FaturaMapper::paraDominio).toList();
        }
        return panacheRepository.list("cartaoId = ?1 order by competencia desc", cartaoId)
                .stream().map(FaturaMapper::paraDominio).toList();
    }

    @Override
    public List<Fatura> listarAbertasVencidas(LocalDate hoje) {
        return panacheRepository.list("status = ?1 and dataFechamento <= ?2", StatusFatura.ABERTA, hoje)
                .stream().map(FaturaMapper::paraDominio).toList();
    }

    @Override
    public List<Fatura> listarFechadas() {
        return panacheRepository.list("status = ?1", StatusFatura.FECHADA)
                .stream().map(FaturaMapper::paraDominio).toList();
    }
}
