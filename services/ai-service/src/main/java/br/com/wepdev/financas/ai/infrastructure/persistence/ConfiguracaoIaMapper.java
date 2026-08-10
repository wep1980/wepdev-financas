package br.com.wepdev.financas.ai.infrastructure.persistence;

import br.com.wepdev.financas.ai.domain.ConfiguracaoIa;
import br.com.wepdev.financas.ai.domain.ProvedorIa;

/** Cuida só dos campos não sensíveis — apiKey (que precisa criptografar/descriptografar) é responsabilidade do RepositoryImpl, que tem acesso ao CriptografiaService. */
final class ConfiguracaoIaMapper {

    private ConfiguracaoIaMapper() {
    }

    static ConfiguracaoIaEntity paraNovaEntidade(ConfiguracaoIa configuracao) {
        ConfiguracaoIaEntity entity = new ConfiguracaoIaEntity();
        entity.usuarioId = configuracao.getUsuarioId();
        atualizarEntidadeSemApiKey(entity, configuracao);
        return entity;
    }

    static void atualizarEntidadeSemApiKey(ConfiguracaoIaEntity entity, ConfiguracaoIa configuracao) {
        entity.provedor = configuracao.getProvedor().name();
        entity.ollamaUrl = configuracao.getOllamaUrl();
    }

    static ConfiguracaoIa paraDominio(ConfiguracaoIaEntity entity, String apiKeyDescriptografada) {
        return ConfiguracaoIa.reconstituir(
                entity.usuarioId,
                ProvedorIa.valueOf(entity.provedor),
                apiKeyDescriptografada,
                entity.ollamaUrl
        );
    }
}
