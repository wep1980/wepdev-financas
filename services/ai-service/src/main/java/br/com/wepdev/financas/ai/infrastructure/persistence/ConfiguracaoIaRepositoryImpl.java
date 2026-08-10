package br.com.wepdev.financas.ai.infrastructure.persistence;

import br.com.wepdev.financas.ai.domain.ConfiguracaoIa;
import br.com.wepdev.financas.ai.domain.ConfiguracaoIaRepository;
import br.com.wepdev.financas.ai.infrastructure.security.CriptografiaService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ConfiguracaoIaRepositoryImpl implements ConfiguracaoIaRepository {

    private final ConfiguracaoIaMongoRepository mongoRepository;
    private final CriptografiaService criptografiaService;

    public ConfiguracaoIaRepositoryImpl(ConfiguracaoIaMongoRepository mongoRepository, CriptografiaService criptografiaService) {
        this.mongoRepository = mongoRepository;
        this.criptografiaService = criptografiaService;
    }

    @Override
    public void salvar(ConfiguracaoIa configuracao) {
        ConfiguracaoIaEntity entity = mongoRepository.findById(configuracao.getUsuarioId());
        if (entity == null) {
            entity = ConfiguracaoIaMapper.paraNovaEntidade(configuracao);
            entity.apiKeyCriptografada = criptografiaService.criptografar(configuracao.getApiKey());
            mongoRepository.persist(entity);
        } else {
            ConfiguracaoIaMapper.atualizarEntidadeSemApiKey(entity, configuracao);
            entity.apiKeyCriptografada = criptografiaService.criptografar(configuracao.getApiKey());
            mongoRepository.update(entity);
        }
    }

    @Override
    public Optional<ConfiguracaoIa> buscarPorUsuario(UUID usuarioId) {
        ConfiguracaoIaEntity entity = mongoRepository.findById(usuarioId);
        if (entity == null) {
            return Optional.empty();
        }
        String apiKey = criptografiaService.descriptografar(entity.apiKeyCriptografada);
        return Optional.of(ConfiguracaoIaMapper.paraDominio(entity, apiKey));
    }
}
