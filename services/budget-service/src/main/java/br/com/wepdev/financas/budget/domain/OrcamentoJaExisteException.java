package br.com.wepdev.financas.budget.domain;

import java.time.YearMonth;

/** Já existe um orçamento ATIVO pra essa categoria nesse mês (usuarioId + categoria + mesReferencia é único entre os ativos). */
public class OrcamentoJaExisteException extends RuntimeException {

    public OrcamentoJaExisteException(String categoria, YearMonth mesReferencia) {
        super("Já existe orçamento ativo para a categoria '" + categoria + "' em " + mesReferencia);
    }
}
