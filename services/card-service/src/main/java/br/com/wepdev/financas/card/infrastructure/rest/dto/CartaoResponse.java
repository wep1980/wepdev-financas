package br.com.wepdev.financas.card.infrastructure.rest.dto;

import br.com.wepdev.financas.card.domain.Bandeira;
import br.com.wepdev.financas.card.domain.Cartao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartaoResponse(
        UUID id,
        UUID usuarioId,
        String apelido,
        Bandeira bandeira,
        BigDecimal limite,
        int diaFechamento,
        int diaVencimento,
        UUID contaPagamentoId,
        boolean ativo,
        Instant criadoEm
) {
    public static CartaoResponse de(Cartao cartao) {
        return new CartaoResponse(
                cartao.getId(),
                cartao.getUsuarioId(),
                cartao.getApelido(),
                cartao.getBandeira(),
                cartao.getLimite(),
                cartao.getDiaFechamento(),
                cartao.getDiaVencimento(),
                cartao.getContaPagamentoId(),
                cartao.isAtivo(),
                cartao.getCriadoEm()
        );
    }
}
