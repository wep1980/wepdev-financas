package br.com.wepdev.financas.ai.application;

import br.com.wepdev.financas.ai.domain.Conversa;
import br.com.wepdev.financas.ai.domain.ConversaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListarConversasUseCase {

    private final ConversaRepository conversaRepository;

    public ListarConversasUseCase(ConversaRepository conversaRepository) {
        this.conversaRepository = conversaRepository;
    }

    public List<Conversa> executar(UUID usuarioId) {
        return conversaRepository.listarPorUsuario(usuarioId);
    }
}
