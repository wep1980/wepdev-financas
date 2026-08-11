package br.com.wepdev.financas.document.infrastructure.rest.dto;

import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.StatusDocumento;
import br.com.wepdev.financas.document.domain.TipoDocumento;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentoImportadoResponse(
        UUID id,
        UUID usuarioId,
        TipoDocumento tipo,
        UUID cartaoId,
        String nomeArquivo,
        StatusDocumento status,
        String mensagemErro,
        List<LancamentoPendenteResponse> lancamentos,
        Instant criadoEm,
        Instant processadoEm
) {
    public static DocumentoImportadoResponse de(DocumentoImportado documento) {
        return new DocumentoImportadoResponse(
                documento.getId(),
                documento.getUsuarioId(),
                documento.getTipo(),
                documento.getCartaoId(),
                documento.getNomeArquivo(),
                documento.getStatus(),
                documento.getMensagemErro(),
                documento.getLancamentos().stream().map(LancamentoPendenteResponse::de).toList(),
                documento.getCriadoEm(),
                documento.getProcessadoEm()
        );
    }
}
