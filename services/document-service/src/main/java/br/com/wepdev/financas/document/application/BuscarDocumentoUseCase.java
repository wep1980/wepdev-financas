package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.DocumentoNaoEncontradoException;
import br.com.wepdev.financas.document.domain.DocumentoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class BuscarDocumentoUseCase {

    private final DocumentoRepository repository;

    public BuscarDocumentoUseCase(DocumentoRepository repository) {
        this.repository = repository;
    }

    public DocumentoImportado executar(UUID id, UUID usuarioId) {
        return repository.buscarPorId(id, usuarioId)
                .orElseThrow(() -> new DocumentoNaoEncontradoException(id));
    }
}
