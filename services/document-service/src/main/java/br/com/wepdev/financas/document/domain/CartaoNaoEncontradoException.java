package br.com.wepdev.financas.document.domain;

import java.util.UUID;

/** Cartão inexistente OU que não pertence ao usuário autenticado — mesmo erro nos dois casos (evita IDOR). */
public class CartaoNaoEncontradoException extends RuntimeException {

    public CartaoNaoEncontradoException(UUID id) {
        super("Cartão não encontrado: " + id);
    }
}
