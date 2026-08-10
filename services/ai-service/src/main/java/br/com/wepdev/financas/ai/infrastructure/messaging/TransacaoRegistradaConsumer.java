package br.com.wepdev.financas.ai.infrastructure.messaging;

import br.com.wepdev.financas.ai.application.IndexarTransacaoComando;
import br.com.wepdev.financas.ai.application.IndexarTransacaoUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class TransacaoRegistradaConsumer {

    private final IndexarTransacaoUseCase useCase;

    public TransacaoRegistradaConsumer(IndexarTransacaoUseCase useCase) {
        this.useCase = useCase;
    }

    @Incoming("transacao-eventos")
    public void consumir(TransacaoRegistradaEvento evento) {
        useCase.executar(new IndexarTransacaoComando(evento.transacaoId(), evento.usuarioId(), evento.descricao()));
    }
}
