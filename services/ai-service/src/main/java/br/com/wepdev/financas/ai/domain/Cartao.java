package br.com.wepdev.financas.ai.domain;

import java.util.UUID;

/** Leitura do card-service — só o que a tool buscar_fatura_cartao precisa pra resolver "cartão X" por apelido. */
public record Cartao(UUID id, String apelido) {
}
