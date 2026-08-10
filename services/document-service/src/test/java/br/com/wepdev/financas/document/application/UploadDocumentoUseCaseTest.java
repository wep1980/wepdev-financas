package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.DocumentoRepository;
import br.com.wepdev.financas.document.domain.StatusDocumento;
import br.com.wepdev.financas.document.domain.TipoDocumento;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadDocumentoUseCaseTest {

    private final DocumentoRepository repository = mock(DocumentoRepository.class);
    private final ProcessarDocumentoService processador = mock(ProcessarDocumentoService.class);
    private final ManagedExecutor executor = mock(ManagedExecutor.class);
    private final UploadDocumentoUseCase useCase = new UploadDocumentoUseCase(repository, processador, executor);

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void deveriaPersistirComoRecebido_eDespacharProcessamentoEmBackground() {
        when(executor.runAsync(any(Runnable.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return CompletableFuture.completedFuture(null);
        });

        UploadDocumentoCommand command = new UploadDocumentoCommand(
                usuarioId, TipoDocumento.FATURA_CARTAO, "fatura.pdf", "conteudo-pdf".getBytes(), "senha123", "JOAO");

        DocumentoImportado documento = useCase.executar(command);

        assertThat(documento.getStatus()).isEqualTo(StatusDocumento.RECEBIDO);
        assertThat(documento.getUsuarioId()).isEqualTo(usuarioId);
        verify(repository).salvar(documento);
        verify(processador).processar(documento.getId(), usuarioId, command.conteudoArquivo(), "senha123", "JOAO");
    }

    @Test
    void naoDeveriaDespacharProcessamento_antesDeSalvarRetornar() {
        // repository.salvar() é chamado antes de executor.runAsync() — a ordem
        // importa (ADR-0024): garante que o documento já está commitado antes
        // do job assíncrono tentar buscá-lo numa outra thread.
        var ordem = org.mockito.Mockito.inOrder(repository, executor);
        when(executor.runAsync(any(Runnable.class))).thenReturn(CompletableFuture.completedFuture(null));

        useCase.executar(new UploadDocumentoCommand(
                usuarioId, TipoDocumento.FATURA_CARTAO, "fatura.pdf", "conteudo".getBytes(), null, null));

        ordem.verify(repository).salvar(any());
        ordem.verify(executor).runAsync(any(Runnable.class));
    }
}
