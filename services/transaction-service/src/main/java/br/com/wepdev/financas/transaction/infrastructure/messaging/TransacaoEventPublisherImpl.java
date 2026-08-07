package br.com.wepdev.financas.transaction.infrastructure.messaging;

import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoEventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class TransacaoEventPublisherImpl implements TransacaoEventPublisher {

    @Channel("transacao-eventos")
    Emitter<TransacaoRegistradaEvento> emitter;

    @Override
    public void publicarTransacaoRegistrada(Transacao transacao) {
        emitter.send(new TransacaoRegistradaEvento(
                transacao.getId(),
                transacao.getContaId(),
                transacao.getUsuarioId(),
                transacao.getTipo().name(),
                transacao.getValor(),
                transacao.getCriadoEm()
        ));
    }
}
