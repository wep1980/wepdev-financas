package br.com.wepdev.financas.transaction.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída (Dependency Inversion) — domínio define o contrato, infraestrutura implementa. */
public interface TransacaoRepository {

    /** Insere se for nova, atualiza se já existir (upsert por id). */
    void salvar(Transacao transacao);

    Optional<Transacao> buscarPorId(UUID id);

    List<Transacao> listar(TransacaoFiltro filtro);
}
