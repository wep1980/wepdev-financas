package br.com.wepdev.financas.budget.domain;

import java.util.Optional;
import java.util.UUID;

/** Porta de saída (Dependency Inversion) — domínio define o contrato, infraestrutura implementa. */
public interface ReservaRepository {

    /** Insere se for novo, atualiza se já existir (upsert por usuarioId). */
    void salvar(Reserva reserva);

    Optional<Reserva> buscarPorUsuario(UUID usuarioId);
}
