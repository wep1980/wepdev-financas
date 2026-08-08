package br.com.wepdev.financas.transaction.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída (Dependency Inversion) — domínio define o contrato, infraestrutura implementa. */
public interface TransacaoRecorrenteRepository {

    /** Insere se for nova, atualiza se já existir (upsert por id). */
    void salvar(TransacaoRecorrente regra);

    Optional<TransacaoRecorrente> buscarPorId(UUID id);

    /** status null = sem filtro (todas as regras do usuário, qualquer status). */
    List<TransacaoRecorrente> listar(UUID usuarioId, StatusTransacaoRecorrente status);

    /** Todas as regras ATIVA de todos os usuários — usado pelo job de geração de ocorrências. */
    List<TransacaoRecorrente> listarAtivas();
}
