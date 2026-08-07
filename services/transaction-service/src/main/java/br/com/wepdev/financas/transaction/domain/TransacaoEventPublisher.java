package br.com.wepdev.financas.transaction.domain;

public interface TransacaoEventPublisher {

    void publicarTransacaoRegistrada(Transacao transacao);
}
