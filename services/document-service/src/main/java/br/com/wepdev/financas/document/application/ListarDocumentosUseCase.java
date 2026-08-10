package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.DocumentoRepository;
import br.com.wepdev.financas.document.domain.StatusDocumento;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListarDocumentosUseCase {

    private final DocumentoRepository repository;

    public ListarDocumentosUseCase(DocumentoRepository repository) {
        this.repository = repository;
    }

    public List<DocumentoImportado> executar(UUID usuarioId, StatusDocumento statusFiltro) {
        return repository.listarPorUsuario(usuarioId, statusFiltro);
    }
}
