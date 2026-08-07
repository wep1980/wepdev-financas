package br.com.wepdev.financas.account.infrastructure.persistence;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.ContaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ContaRepositoryImpl implements ContaRepository {

    private final ContaPanacheRepository panacheRepository;

    public ContaRepositoryImpl(ContaPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public void salvar(Conta conta) {
        ContaEntity entity = panacheRepository.findById(conta.getId());
        if (entity == null) {
            panacheRepository.persist(ContaMapper.paraNovaEntidade(conta));
        } else {
            ContaMapper.atualizarEntidade(entity, conta);
        }
    }

    @Override
    public Optional<Conta> buscarPorId(UUID id) {
        return panacheRepository.findByIdOptional(id).map(ContaMapper::paraDominio);
    }

    @Override
    public List<Conta> listarAtivasPorUsuario(UUID usuarioId) {
        return panacheRepository.list("usuarioId = ?1 and ativa = true", usuarioId)
                .stream()
                .map(ContaMapper::paraDominio)
                .toList();
    }
}
