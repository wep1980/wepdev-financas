package br.com.wepdev.financas.account.infrastructure.messaging;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.ContaEventPublisher;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ContaEventPublisherImpl implements ContaEventPublisher {

    @Channel("conta-eventos")
    Emitter<ContaCriadaEvento> emitter;

    @Override
    public void publicarContaCriada(Conta conta) {
        emitter.send(new ContaCriadaEvento(
                conta.getId(),
                conta.getUsuarioId(),
                conta.getTipo().name(),
                conta.getSaldo(),
                conta.getCriadoEm()
        ));
    }
}
