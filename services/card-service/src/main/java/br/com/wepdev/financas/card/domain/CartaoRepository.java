package br.com.wepdev.financas.card.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída (Dependency Inversion) — domínio define o contrato, infraestrutura implementa. */
public interface CartaoRepository {

    /** Insere se for novo, atualiza se já existir (upsert por id). */
    void salvar(Cartao cartao);

    Optional<Cartao> buscarPorId(UUID id);

    /** Só os cartões ativos do usuário — exclusão lógica não aparece na listagem. */
    List<Cartao> listarAtivos(UUID usuarioId);
}
