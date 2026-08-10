package br.com.wepdev.financas.document.domain;

import java.util.UUID;

/** Conta inexistente OU que não pertence ao usuário autenticado — mesmo erro nos dois casos (evita IDOR). */
public class ContaNaoEncontradaException extends RuntimeException {

    public ContaNaoEncontradaException(UUID id) {
        super("Conta não encontrada: " + id);
    }
}
