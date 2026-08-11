package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.DocumentoRepository;
import br.com.wepdev.financas.document.domain.StatusDocumento;
import br.com.wepdev.financas.document.domain.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarDocumentosUseCaseTest {

    private final DocumentoRepository repository = mock(DocumentoRepository.class);
    private final ListarDocumentosUseCase useCase = new ListarDocumentosUseCase(repository);

    @Test
    void deveriaDelegarParaORepositorio_comOFiltroDeStatus() {
        UUID usuarioId = UUID.randomUUID();
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO,
                UUID.randomUUID(), "a.pdf", "x".getBytes());
        when(repository.listarPorUsuario(usuarioId, StatusDocumento.AGUARDANDO_CONFIRMACAO)).thenReturn(List.of(documento));

        List<DocumentoImportado> resultado = useCase.executar(usuarioId, StatusDocumento.AGUARDANDO_CONFIRMACAO);

        assertThat(resultado).containsExactly(documento);
    }
}
