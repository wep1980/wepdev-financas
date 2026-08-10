package br.com.wepdev.financas.ai.application;

import br.com.wepdev.financas.ai.domain.ProvedorIa;

import java.util.UUID;

public record DefinirConfiguracaoIaComando(UUID usuarioId, ProvedorIa provedor, String apiKey, String ollamaUrl) {
}
