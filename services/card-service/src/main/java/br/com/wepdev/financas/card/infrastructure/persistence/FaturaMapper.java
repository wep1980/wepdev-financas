package br.com.wepdev.financas.card.infrastructure.persistence;

import br.com.wepdev.financas.card.domain.Fatura;

import java.time.YearMonth;

final class FaturaMapper {

    private FaturaMapper() {
    }

    static FaturaEntity paraNovaEntidade(Fatura fatura) {
        FaturaEntity entity = new FaturaEntity();
        entity.id = fatura.getId();
        atualizarEntidade(entity, fatura);
        return entity;
    }

    static void atualizarEntidade(FaturaEntity entity, Fatura fatura) {
        entity.cartaoId = fatura.getCartaoId();
        entity.usuarioId = fatura.getUsuarioId();
        entity.competencia = fatura.getCompetencia().toString();
        entity.dataFechamento = fatura.getDataFechamento();
        entity.dataVencimento = fatura.getDataVencimento();
        entity.valorTotal = fatura.getValorTotal();
        entity.status = fatura.getStatus();
    }

    static Fatura paraDominio(FaturaEntity entity) {
        return Fatura.reconstituir(
                entity.id,
                entity.cartaoId,
                entity.usuarioId,
                YearMonth.parse(entity.competencia),
                entity.dataFechamento,
                entity.dataVencimento,
                entity.valorTotal,
                entity.status
        );
    }
}
