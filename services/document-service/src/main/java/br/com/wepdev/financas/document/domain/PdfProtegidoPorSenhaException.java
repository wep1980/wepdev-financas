package br.com.wepdev.financas.document.domain;

/** PDF criptografado — senha não fornecida ou incorreta. Muito comum em fatura de banco/cartão brasileira (ex: CPF do titular). */
public class PdfProtegidoPorSenhaException extends RuntimeException {

    public PdfProtegidoPorSenhaException() {
        super("PDF protegido por senha — senha não fornecida ou incorreta");
    }
}
