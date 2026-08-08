package br.com.wepdev.financas.transaction.domain;

import java.time.LocalDate;

public class IntervaloInvalidoException extends RuntimeException {

    public IntervaloInvalidoException(LocalDate inicio, LocalDate fim) {
        super("Data de início (" + inicio + ") não pode ser depois da data de fim (" + fim + ")");
    }
}
