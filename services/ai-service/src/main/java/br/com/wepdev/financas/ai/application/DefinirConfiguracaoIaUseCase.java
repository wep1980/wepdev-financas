package br.com.wepdev.financas.ai.application;

import br.com.wepdev.financas.ai.domain.ConfiguracaoIa;
import br.com.wepdev.financas.ai.domain.ConfiguracaoIaRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefinirConfiguracaoIaUseCase {

    private final ConfiguracaoIaRepository configuracaoIaRepository;

    public DefinirConfiguracaoIaUseCase(ConfiguracaoIaRepository configuracaoIaRepository) {
        this.configuracaoIaRepository = configuracaoIaRepository;
    }

    /** Upsert — primeira vez cria, chamadas seguintes atualizam a mesma configuração (mesmo desenho de DefinirReservaUseCase no budget-service). */
    public ConfiguracaoIa executar(DefinirConfiguracaoIaComando comando) {
        ConfiguracaoIa configuracao = configuracaoIaRepository.buscarPorUsuario(comando.usuarioId()).orElse(null);

        if (configuracao == null) {
            configuracao = ConfiguracaoIa.definir(comando.usuarioId(), comando.provedor(), comando.apiKey(), comando.ollamaUrl());
        } else {
            configuracao.atualizar(comando.provedor(), comando.apiKey(), comando.ollamaUrl());
        }

        configuracaoIaRepository.salvar(configuracao);
        return configuracao;
    }
}
