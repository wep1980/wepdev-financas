package br.com.wepdev.financas.document.domain;

import java.util.UUID;

/** Só dá pra confirmar lançamentos depois que o documento terminou de processar (status AGUARDANDO_CONFIRMACAO). */
public class DocumentoAindaNaoProcessadoException extends RuntimeException {

    public DocumentoAindaNaoProcessadoException(UUID id) {
        super("Documento ainda não terminou de processar, não pode confirmar lançamentos: " + id);
    }
}
