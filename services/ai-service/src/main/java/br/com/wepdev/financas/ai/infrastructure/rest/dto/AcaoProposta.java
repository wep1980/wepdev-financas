package br.com.wepdev.financas.ai.infrastructure.rest.dto;

import br.com.wepdev.financas.ai.domain.AcaoPendente;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AcaoProposta(
        String tipo,
        String descricao,
        BigDecimal valor,
        boolean recorrente,
        String frequencia,
        Integer quantidadeOcorrencias,
        UUID contaId,
        String categoria,
        Instant expiraEm
) {
    public static AcaoProposta de(AcaoPendente acao) {
        return new AcaoProposta(
                acao.getTipo().name(),
                acao.getDescricao(),
                acao.getValor(),
                acao.isRecorrente(),
                acao.getFrequencia() == null ? null : acao.getFrequencia().name(),
                acao.getQuantidadeOcorrencias(),
                acao.getContaId(),
                acao.getCategoria(),
                acao.getExpiraEm()
        );
    }
}
