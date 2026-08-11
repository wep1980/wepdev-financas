package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.DocumentoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Persiste o documento como RECEBIDO e só DEPOIS despacha o processamento
 * em background (ADR-0024) — nunca o contrário: {@code repository.salvar()}
 * é transacional e só retorna depois de commitar, então quando o
 * {@code ManagedExecutor} dispara {@link ProcessarDocumentoService} numa
 * outra thread, o documento já está garantidamente visível no banco pra
 * ela buscar.
 */
@ApplicationScoped
public class UploadDocumentoUseCase {

    private static final Logger LOG = Logger.getLogger(UploadDocumentoUseCase.class);

    private final DocumentoRepository repository;
    private final ProcessarDocumentoService processador;
    private final ManagedExecutor executor;

    public UploadDocumentoUseCase(DocumentoRepository repository, ProcessarDocumentoService processador,
                                   ManagedExecutor executor) {
        this.repository = repository;
        this.processador = processador;
        this.executor = executor;
    }

    public DocumentoImportado executar(UploadDocumentoCommand command) {
        DocumentoImportado documento = DocumentoImportado.receber(
                command.usuarioId(), command.tipo(), command.cartaoId(), command.nomeArquivo(),
                command.conteudoArquivo());
        repository.salvar(documento);

        UUID documentoId = documento.getId();
        UUID usuarioId = command.usuarioId();
        byte[] conteudoArquivo = command.conteudoArquivo();
        String senha = command.senha();
        String nomeFiltro = command.nomeFiltro();
        executor.runAsync(() -> processador.processar(documentoId, usuarioId, conteudoArquivo, senha, nomeFiltro))
                .exceptionally(e -> {
                    LOG.error("Falha inesperada ao despachar processamento do documento " + documentoId, e);
                    return null;
                });

        return documento;
    }
}
