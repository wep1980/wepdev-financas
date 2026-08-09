package br.com.wepdev.financas.card.domain;

import java.util.UUID;

/** Fatura ainda ABERTA não tem valorTotal definitivo — só dá pra pagar depois de fechar (FECHADA). */
public class FaturaAindaAbertaException extends RuntimeException {

    public FaturaAindaAbertaException(UUID id) {
        super("Fatura ainda está aberta, não pode ser paga: " + id);
    }
}
