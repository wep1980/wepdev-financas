package br.com.wepdev.financas.ai.application;

import br.com.wepdev.financas.ai.domain.Conversa;
import br.com.wepdev.financas.ai.domain.ConversaNaoEncontradaException;
import br.com.wepdev.financas.ai.domain.ConversaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class BuscarConversaUseCase {

    private final ConversaRepository conversaRepository;

    public BuscarConversaUseCase(ConversaRepository conversaRepository) {
        this.conversaRepository = conversaRepository;
    }

    public Conversa executar(UUID id, UUID usuarioId) {
        return conversaRepository.buscarPorId(id)
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ConversaNaoEncontradaException(id));
    }
}
