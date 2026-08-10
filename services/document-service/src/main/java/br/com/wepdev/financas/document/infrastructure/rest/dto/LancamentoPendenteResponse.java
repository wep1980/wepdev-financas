package br.com.wepdev.financas.document.infrastructure.rest.dto;

import br.com.wepdev.financas.document.domain.LancamentoPendente;
import br.com.wepdev.financas.document.domain.StatusLancamento;
import br.com.wepdev.financas.document.domain.TipoLancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LancamentoPendenteResponse(
        UUID id,
        UUID documentoId,
        String descricao,
        BigDecimal valor,
        LocalDate data,
        TipoLancamento tipo,
        String categoriaSugerida,
        StatusLancamento status
) {
    public static LancamentoPendenteResponse de(LancamentoPendente lancamento) {
        return new LancamentoPendenteResponse(
                lancamento.getId(),
                lancamento.getDocumentoId(),
                lancamento.getDescricao(),
                lancamento.getValor(),
                lancamento.getData(),
                lancamento.getTipo(),
                lancamento.getCategoriaSugerida(),
                lancamento.getStatus()
        );
    }
}
