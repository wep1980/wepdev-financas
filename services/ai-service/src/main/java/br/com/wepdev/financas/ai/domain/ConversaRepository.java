package br.com.wepdev.financas.ai.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída (Dependency Inversion) — domínio define o contrato, infraestrutura implementa. */
public interface ConversaRepository {

    /** Insere se for nova, atualiza se já existir (upsert por id). */
    void salvar(Conversa conversa);

    Optional<Conversa> buscarPorId(UUID id);

    /** Todas as conversas do usuário, mais recente primeiro (por última atividade). */
    List<Conversa> listarPorUsuario(UUID usuarioId);
}
