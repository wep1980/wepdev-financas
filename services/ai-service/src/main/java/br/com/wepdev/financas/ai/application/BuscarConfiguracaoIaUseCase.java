package br.com.wepdev.financas.ai.application;

import br.com.wepdev.financas.ai.domain.ConfiguracaoIa;
import br.com.wepdev.financas.ai.domain.ConfiguracaoIaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class BuscarConfiguracaoIaUseCase {

    private final ConfiguracaoIaRepository configuracaoIaRepository;

    public BuscarConfiguracaoIaUseCase(ConfiguracaoIaRepository configuracaoIaRepository) {
        this.configuracaoIaRepository = configuracaoIaRepository;
    }

    /** Nunca 404 — usuário que nunca configurou recebe ConfiguracaoIa.semDefinir (provedor NENHUM). */
    public ConfiguracaoIa executar(UUID usuarioId) {
        return configuracaoIaRepository.buscarPorUsuario(usuarioId).orElseGet(() -> ConfiguracaoIa.semDefinir(usuarioId));
    }
}
