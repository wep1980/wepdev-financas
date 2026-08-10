package br.com.wepdev.financas.document.domain;

/** Confirmar sem selecionar nenhum lançamento não faz sentido — se o usuário não quer nada, o fluxo é simplesmente não confirmar. */
public class NenhumLancamentoSelecionadoException extends RuntimeException {

    public NenhumLancamentoSelecionadoException() {
        super("Nenhum lançamento foi selecionado para confirmação");
    }
}
