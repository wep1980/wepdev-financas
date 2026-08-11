package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.TipoDocumento;

import java.util.UUID;

public record UploadDocumentoCommand(
        UUID usuarioId,
        TipoDocumento tipo,
        UUID cartaoId,
        String nomeArquivo,
        byte[] conteudoArquivo,
        String senha,
        String nomeFiltro
) {
}
