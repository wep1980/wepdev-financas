package br.com.wepdev.financas.ai.domain;

import java.util.UUID;

/** Leitura do account-service — só o que o agente precisa pra resolver "conta corrente"/"carteira" (texto livre) pro id de verdade. */
public record Conta(UUID id, String nome) {
}
