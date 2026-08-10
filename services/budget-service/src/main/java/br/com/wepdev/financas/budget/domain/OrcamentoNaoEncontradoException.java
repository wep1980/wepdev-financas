package br.com.wepdev.financas.budget.domain;

import java.util.UUID;

/** Orçamento inexistente OU que não pertence ao usuário autenticado — mesmo erro nos dois casos (evita IDOR). */
public class OrcamentoNaoEncontradoException extends RuntimeException {

    public OrcamentoNaoEncontradoException(UUID id) {
        super("Orçamento não encontrado: " + id);
    }
}
