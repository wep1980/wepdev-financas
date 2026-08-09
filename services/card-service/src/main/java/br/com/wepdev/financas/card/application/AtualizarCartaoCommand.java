package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Bandeira;

import java.math.BigDecimal;
import java.util.UUID;

public record AtualizarCartaoCommand(
        UUID id,
        UUID usuarioId,
        String apelido,
        Bandeira bandeira,
        BigDecimal limite,
        int diaFechamento,
        int diaVencimento,
        UUID contaPagamentoId
) {
}
