package br.com.wepdev.financas.ai.domain;

import java.util.Optional;
import java.util.UUID;

/** Porta de saída (Dependency Inversion) — domínio define o contrato, infraestrutura implementa. */
public interface ConfiguracaoIaRepository {

    /** Insere se for nova, atualiza se já existir (upsert por usuarioId). */
    void salvar(ConfiguracaoIa configuracao);

    Optional<ConfiguracaoIa> buscarPorUsuario(UUID usuarioId);
}
