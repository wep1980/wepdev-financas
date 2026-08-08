package br.com.wepdev.financas.transaction.infrastructure.persistence;

import br.com.wepdev.financas.transaction.domain.StatusTransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TransacaoRecorrenteRepositoryImpl implements TransacaoRecorrenteRepository {

    private final TransacaoRecorrentePanacheRepository panacheRepository;

    public TransacaoRecorrenteRepositoryImpl(TransacaoRecorrentePanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public void salvar(TransacaoRecorrente regra) {
        TransacaoRecorrenteEntity entity = panacheRepository.findById(regra.getId());
        if (entity == null) {
            panacheRepository.persist(TransacaoRecorrenteMapper.paraNovaEntidade(regra));
        } else {
            TransacaoRecorrenteMapper.atualizarEntidade(entity, regra);
        }
    }

    @Override
    public Optional<TransacaoRecorrente> buscarPorId(UUID id) {
        return panacheRepository.findByIdOptional(id).map(TransacaoRecorrenteMapper::paraDominio);
    }

    @Override
    public List<TransacaoRecorrente> listar(UUID usuarioId, StatusTransacaoRecorrente status) {
        if (status != null) {
            return panacheRepository.list("usuarioId = ?1 and status = ?2 order by criadoEm desc", usuarioId, status)
                    .stream().map(TransacaoRecorrenteMapper::paraDominio).toList();
        }
        return panacheRepository.list("usuarioId = ?1 order by criadoEm desc", usuarioId)
                .stream().map(TransacaoRecorrenteMapper::paraDominio).toList();
    }

    @Override
    public List<TransacaoRecorrente> listarAtivas() {
        return panacheRepository.list("status = ?1", StatusTransacaoRecorrente.ATIVA)
                .stream().map(TransacaoRecorrenteMapper::paraDominio).toList();
    }
}
