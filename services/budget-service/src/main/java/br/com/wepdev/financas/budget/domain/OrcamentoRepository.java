package br.com.wepdev.financas.budget.domain;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída (Dependency Inversion) — domínio define o contrato, infraestrutura implementa. */
public interface OrcamentoRepository {

    /** Insere se for novo, atualiza se já existir (upsert por id). */
    void salvar(Orcamento orcamento);

    Optional<Orcamento> buscarPorId(UUID id);

    /** Só os orçamentos ativos do usuário nesse mês — exclusão lógica não aparece na listagem. */
    List<Orcamento> listarAtivos(UUID usuarioId, YearMonth mesReferencia);

    /** Usado pelo caso de uso de criação pra rejeitar duplicata antes de inserir (OrcamentoJaExisteException). */
    boolean existeAtivo(UUID usuarioId, String categoria, YearMonth mesReferencia);
}
