package br.com.wepdev.financas.ai.infrastructure.persistence;

import br.com.wepdev.financas.ai.domain.Conversa;
import br.com.wepdev.financas.ai.domain.ConversaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ConversaRepositoryImpl implements ConversaRepository {

    private final ConversaMongoRepository mongoRepository;

    public ConversaRepositoryImpl(ConversaMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public void salvar(Conversa conversa) {
        ConversaEntity entity = mongoRepository.findById(conversa.getId());
        if (entity == null) {
            mongoRepository.persist(ConversaMapper.paraNovaEntidade(conversa));
        } else {
            ConversaMapper.atualizarEntidade(entity, conversa);
            mongoRepository.update(entity);
        }
    }

    @Override
    public Optional<Conversa> buscarPorId(UUID id) {
        return Optional.ofNullable(mongoRepository.findById(id)).map(ConversaMapper::paraDominio);
    }

    @Override
    public List<Conversa> listarPorUsuario(UUID usuarioId) {
        return mongoRepository.list("usuarioId", usuarioId).stream()
                .map(ConversaMapper::paraDominio)
                .sorted(Comparator.comparing(Conversa::getUltimaAtividadeEm).reversed())
                .toList();
    }
}
