package br.com.wepdev.financas.account.domain;

import java.util.UUID;

public class ContaNaoEncontradaException extends RuntimeException {

    public ContaNaoEncontradaException(UUID id) {
        super("Conta não encontrada: " + id);
    }
}
