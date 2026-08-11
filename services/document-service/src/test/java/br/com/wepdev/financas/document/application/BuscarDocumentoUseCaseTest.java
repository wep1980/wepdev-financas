package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.DocumentoNaoEncontradoException;
import br.com.wepdev.financas.document.domain.DocumentoRepository;
import br.com.wepdev.financas.document.domain.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuscarDocumentoUseCaseTest {

    private final DocumentoRepository repository = mock(DocumentoRepository.class);
    private final BuscarDocumentoUseCase useCase = new BuscarDocumentoUseCase(repository);

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID cartaoId = UUID.randomUUID();

    @Test
    void deveriaRetornarDocumento_quandoEncontrado() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO, cartaoId, "a.pdf", "x".getBytes());
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));

        assertThat(useCase.executar(documento.getId(), usuarioId)).isEqualTo(documento);
    }

    @Test
    void deveriaLancarExcecao_quandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id, usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, usuarioId))
                .isInstanceOf(DocumentoNaoEncontradoException.class);
    }
}
