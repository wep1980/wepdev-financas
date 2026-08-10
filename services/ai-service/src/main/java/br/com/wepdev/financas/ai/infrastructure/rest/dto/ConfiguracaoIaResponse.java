package br.com.wepdev.financas.ai.infrastructure.rest.dto;

import br.com.wepdev.financas.ai.domain.ConfiguracaoIa;

public record ConfiguracaoIaResponse(String provedor, boolean configurado, String ollamaUrl) {
    public static ConfiguracaoIaResponse de(ConfiguracaoIa configuracao) {
        return new ConfiguracaoIaResponse(configuracao.getProvedor().name(), configuracao.isConfigurado(), configuracao.getOllamaUrl());
    }
}
