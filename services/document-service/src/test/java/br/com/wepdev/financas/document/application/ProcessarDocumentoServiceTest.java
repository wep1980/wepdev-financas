package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.DocumentoNaoEncontradoException;
import br.com.wepdev.financas.document.domain.DocumentoRepository;
import br.com.wepdev.financas.document.domain.LancamentoPendente;
import br.com.wepdev.financas.document.domain.PdfIlegivelException;
import br.com.wepdev.financas.document.domain.StatusDocumento;
import br.com.wepdev.financas.document.domain.TipoDocumento;
import br.com.wepdev.financas.document.domain.TipoLancamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessarDocumentoServiceTest {

    private final DocumentoRepository repository = mock(DocumentoRepository.class);
    private final AgenteExtracaoFaturaService agente = mock(AgenteExtracaoFaturaService.class);
    private final ProcessarDocumentoService service = new ProcessarDocumentoService(repository, agente);

    private final UUID usuarioId = UUID.randomUUID();
    private final byte[] conteudo = "pdf-fake".getBytes();

    @Test
    void deveriaConcluirComLancamentos_quandoAgenteExtraiAlgo() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO, "fatura.pdf", conteudo);
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));
        LancamentoPendente lancamento = LancamentoPendente.extrair(documento.getId(), "Mercado",
                new BigDecimal("100.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null);
        when(agente.extrair(documento.getId(), conteudo, null, null)).thenReturn(List.of(lancamento));

        service.processar(documento.getId(), usuarioId, conteudo, null, null);

        assertThat(documento.getStatus()).isEqualTo(StatusDocumento.AGUARDANDO_CONFIRMACAO);
        assertThat(documento.getLancamentos()).hasSize(1);
    }

    @Test
    void deveriaMarcarErro_quandoAgenteNaoExtraiNada() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO, "fatura.pdf", conteudo);
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));
        when(agente.extrair(any(), any(), any(), any())).thenReturn(List.of());

        service.processar(documento.getId(), usuarioId, conteudo, null, null);

        assertThat(documento.getStatus()).isEqualTo(StatusDocumento.ERRO_PROCESSAMENTO);
        assertThat(documento.getMensagemErro()).isEqualTo("Nenhum lançamento reconhecido no documento");
    }

    @Test
    void deveriaMarcarErro_quandoPdfIlegivel() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO, "fatura.pdf", conteudo);
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));
        when(agente.extrair(any(), any(), any(), any())).thenThrow(new PdfIlegivelException());

        service.processar(documento.getId(), usuarioId, conteudo, null, null);

        assertThat(documento.getStatus()).isEqualTo(StatusDocumento.ERRO_PROCESSAMENTO);
        assertThat(documento.getMensagemErro()).isNotBlank();
    }

    @Test
    void deveriaMarcarErroGenerico_quandoAgenteLancaExcecaoInesperada() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO, "fatura.pdf", conteudo);
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));
        when(agente.extrair(any(), any(), any(), any())).thenThrow(new RuntimeException("falha de rede com o Ollama"));

        service.processar(documento.getId(), usuarioId, conteudo, null, null);

        assertThat(documento.getStatus()).isEqualTo(StatusDocumento.ERRO_PROCESSAMENTO);
        assertThat(documento.getMensagemErro()).isEqualTo("Erro inesperado ao processar documento");
    }

    @Test
    void deveriaLancarExcecao_quandoDocumentoNaoEncontrado() {
        UUID documentoId = UUID.randomUUID();
        when(repository.buscarPorId(documentoId, usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processar(documentoId, usuarioId, conteudo, null, null))
                .isInstanceOf(DocumentoNaoEncontradoException.class);
    }
}
