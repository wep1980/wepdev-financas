package br.com.wepdev.financas.card.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída (Dependency Inversion) — domínio define o contrato, infraestrutura implementa. */
public interface FaturaRepository {

    /** Insere se for nova, atualiza se já existir (upsert por id). */
    void salvar(Fatura fatura);

    Optional<Fatura> buscarPorId(UUID id);

    /** Uma fatura por (cartaoId, competencia) — usado por LancarCompraUseCase pra achar ou decidir criar. */
    Optional<Fatura> buscarPorCartaoECompetencia(UUID cartaoId, YearMonth competencia);

    /** status null = sem filtro (todas as faturas do cartão, qualquer status), mais recente primeiro. */
    List<Fatura> listarPorCartao(UUID cartaoId, StatusFatura status);

    /** Todas as faturas ABERTA cuja dataFechamento já passou — usado pelo job de fechamento automático. */
    List<Fatura> listarAbertasVencidas(LocalDate hoje);

    /** Todas as faturas FECHADA (ainda não PAGA) — usado pelo endpoint de próximos vencimentos. */
    List<Fatura> listarFechadas();
}
