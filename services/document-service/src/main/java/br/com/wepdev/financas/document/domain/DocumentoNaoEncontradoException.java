package br.com.wepdev.financas.document.domain;

import java.util.UUID;

/** Documento inexistente OU que não pertence ao usuário autenticado — mesmo erro nos dois casos (evita IDOR). */
public class DocumentoNaoEncontradoException extends RuntimeException {

    public DocumentoNaoEncontradoException(UUID id) {
        super("Documento não encontrado: " + id);
    }
}
