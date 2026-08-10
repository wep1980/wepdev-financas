package br.com.wepdev.financas.ai.application;

import java.util.UUID;

public record IndexarTransacaoComando(UUID transacaoId, UUID usuarioId, String descricao) {
}
