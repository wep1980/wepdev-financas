package br.com.wepdev.financas.document.infrastructure.persistence;

import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.LancamentoPendente;
import br.com.wepdev.financas.document.domain.StatusDocumento;
import br.com.wepdev.financas.document.domain.TipoDocumento;

import java.util.List;

final class DocumentoImportadoMapper {

    private DocumentoImportadoMapper() {
    }

    static DocumentoImportadoEntity paraNovaEntidade(DocumentoImportado documento) {
        DocumentoImportadoEntity entity = new DocumentoImportadoEntity();
        entity.id = documento.getId();
        entity.conteudoArquivo = documento.getConteudoArquivo();
        atualizarEntidade(entity, documento);
        return entity;
    }

    static void atualizarEntidade(DocumentoImportadoEntity entity, DocumentoImportado documento) {
        entity.usuarioId = documento.getUsuarioId();
        entity.tipo = documento.getTipo().name();
        entity.cartaoId = documento.getCartaoId();
        entity.nomeArquivo = documento.getNomeArquivo();
        entity.status = documento.getStatus().name();
        entity.mensagemErro = documento.getMensagemErro();
        entity.criadoEm = documento.getCriadoEm();
        entity.processadoEm = documento.getProcessadoEm();
    }

    /** lancamentos vêm de outro banco (MySQL) — combinados aqui pra reconstruir o agregado completo. */
    static DocumentoImportado paraDominio(DocumentoImportadoEntity entity, List<LancamentoPendente> lancamentos) {
        return DocumentoImportado.reconstituir(
                entity.id,
                entity.usuarioId,
                TipoDocumento.valueOf(entity.tipo),
                entity.cartaoId,
                entity.nomeArquivo,
                entity.conteudoArquivo,
                StatusDocumento.valueOf(entity.status),
                entity.mensagemErro,
                lancamentos,
                entity.criadoEm,
                entity.processadoEm
        );
    }
}
